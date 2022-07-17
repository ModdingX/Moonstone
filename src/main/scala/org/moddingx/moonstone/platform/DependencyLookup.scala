package org.moddingx.moonstone.platform

import com.google.gson.JsonElement
import org.moddingx.moonstone.model.{FileEntry, Side}

import scala.collection.mutable

object DependencyLookup {
  
  def lookupDependencies(list: ModList, installed: Set[FileEntry], dependencies: Set[FileEntry]): Set[FileEntry] = {
    val explicitInstalledIds: Set[JsonElement] = installed.map(_.project)
    val allInstalledIds: Set[JsonElement] = explicitInstalledIds | dependencies.map(_.project)
    
    val dependencyMap = mutable.Map[JsonElement, mutable.Builder[ResolvableDependency, Seq[ResolvableDependency]]]()
    val dependencyState = mutable.Map.from[JsonElement, Side](dependencies.map(entry => (entry.project, entry.side)))
    
    for (dep <- dependencies) {
      // Add already existing dependencies to dependencyMap
      // Further calls to addDependency with these will only affect their side
      // Side is recomputed anyway, so use COMMON here
      dependencyMap.put(dep.project, Seq.newBuilder.addOne(FileDependency(dep)))
    }
    
    def addDependency(dep: ResolvableDependency, side: Side): Unit = {
      if (!explicitInstalledIds.contains(dep.project)) {
        dependencyState.get(dep.project) match {
          case Some(other) => dependencyState.put(dep.project, Side.merge(other, side))
          case None => dependencyState.put(dep.project, side)
        }
      }
      if (!allInstalledIds.contains(dep.project)) {
        dependencyMap.getOrElseUpdate(dep.project, Seq.newBuilder).addOne(dep)
      }
    }
    
    def collectDependencies(file: FileEntry, side: Side): Unit = {
      val cleanedFile = file.withSide(Side.COMMON).withLock(false)
      for (dep <- list.access.dependencies(cleanedFile)) {
        addDependency(dep, side)
        val sideForTransitiveDeps = Side.reduceFrom(side, dep.side)
        dep.file match {
          case Some(transitiveDep) => collectDependencies(transitiveDep, sideForTransitiveDeps)
          case None =>
        }
      }
    }
    
    for (installedFile <- installed) {
      collectDependencies(installedFile, installedFile.side)
    }
    
    val mergedDependencies = mergeDependencies(list, dependencyMap.toMap.map(entry => (entry._1, entry._2.result())))
    mergedDependencies.map(dep => dep.file.withSide(
      Side.reduceFrom(dependencyState.getOrElse(dep.file.project, Side.COMMON), dep.sideRequirement)
    ))
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

  case class ResolvedDependency(file: FileEntry, sideRequirement: Side)
}
