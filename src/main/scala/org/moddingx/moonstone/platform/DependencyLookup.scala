package org.moddingx.moonstone.platform

import com.google.gson.JsonElement
import org.moddingx.moonstone.model.{FileEntry, Side}

import scala.collection.mutable

object DependencyLookup {
  
  def lookupDependencies(list: ModList, installed: Set[FileEntry], dependencies: Set[FileEntry]): Set[FileEntry] = {
    case class ResolvedDependency(file: FileEntry, sideRequirement: Side)
    
    val installedIds: Set[JsonElement] = installed.map(_.project)
    
    val dependencyMap = mutable.Map[JsonElement, mutable.Builder[ResolvableDependency, Seq[ResolvableDependency]]]()
    val dependencyState = mutable.Map[JsonElement, Side]()
    
    val oldDependencyMap: Map[JsonElement, FileEntry] = dependencies.map(entry => (entry.project, entry)).toMap
    
    def addDependency(dep: ResolvableDependency, side: Side): Unit = {
      if (!installedIds.contains(dep.project)) {
        dependencyState.get(dep.project) match {
          case Some(other) => dependencyState.put(dep.project, Side.merge(other, side))
          case None => dependencyState.put(dep.project, side)
        }
        
        oldDependencyMap.get(dep.project) match {
          // Project was a dependency before, take already known version
          case Some(oldDep) => dependencyMap.put(dep.project, Seq.newBuilder.addOne(FileDependency(oldDep, list)))
          case None => dependencyMap.getOrElseUpdate(dep.project, Seq.newBuilder).addOne(dep)
        }
      }
    }
    
    def collectDependencies(file: FileEntry, side: Side): Unit = {
      val cleanedFile = file.withSide(Side.COMMON).withLock(false)
      for (dep <- list.access.dependencies(list.loader, cleanedFile)) {
        addDependency(dep, side)
        val sideForTransitiveDeps = Side.reduceFrom(side, dep.side)
        dep.file match {
          case Some(transitiveDep) => collectDependencies(transitiveDep, sideForTransitiveDeps)
          case None =>
        }
      }
    }

    def mergeDependencies(list: ModList, dependencyMap: Map[JsonElement, Seq[ResolvableDependency]]): Set[ResolvedDependency] = {
      dependencyMap.flatMap(entry => {
        val (project, deps) = entry
        val resolved: Seq[FileEntry] = deps
          .flatMap(_.file)
          .filter(file => file.project == project) // *should* be always true. Just to be sure
          .map(_.withSide(Side.COMMON)) // platform may not set the site through the file entry
          .map(_.withLock(false)) // platform may not set the lock
        val dependencySide: Side = Side.merge(deps.map(_.side): _*)
        list.access.latestFrom(resolved.toSet) match {
          case Some(file) => Some(ResolvedDependency(file, dependencySide))
          case None => None
        }
      }).toSet
    }
    
    for (installedFile <- installed) {
      collectDependencies(installedFile, installedFile.side)
    }
    
    val mergedDependencies = mergeDependencies(list, dependencyMap.toMap.map(entry => (entry._1, entry._2.result())))
    mergedDependencies.map(dep => dep.file.withSide(
      Side.reduceFrom(dependencyState.getOrElse(dep.file.project, Side.COMMON), dep.sideRequirement)
    ))
  }
}
