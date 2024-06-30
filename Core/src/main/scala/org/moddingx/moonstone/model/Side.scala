package org.moddingx.moonstone.model

sealed abstract class Side(val id: String, val client: Boolean, val server: Boolean)

object Side {
  
  val values: Seq[Side] = Seq(COMMON, CLIENT, SERVER)
  
  case object COMMON extends Side("common", true, true)
  case object CLIENT extends Side("client", true, false)
  case object SERVER extends Side("server", false, true)
  
  def byId(id: String): Side = id match {
    case COMMON.id => COMMON
    case CLIENT.id => CLIENT
    case SERVER.id => SERVER
    case id => System.err.print("Side not found: " + id); COMMON
  }
  
  def get(client: Boolean, server: Boolean): Side = {
    if (client && server) COMMON
    else if (client) CLIENT
    else if (server) SERVER
    else COMMON
  }
  
  def merge(sides: Side*): Side = {
    if (sides.isEmpty) {
      COMMON
    } else {
      sides.reduce((s1, s2) => if (s1 == s2) s1 else COMMON)
    }
  }
  
  def reduceFrom(parent: Side, child: Side): Side = parent match {
    case COMMON => child
    case _ => parent
  }
}
