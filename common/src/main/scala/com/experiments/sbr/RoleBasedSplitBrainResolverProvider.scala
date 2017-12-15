package com.experiments.sbr

import akka.actor.ActorSystem
import akka.cluster.DowningProvider
import akka.actor.Props
import com.typesafe.config.Config

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.collection.JavaConverters._

final class RoleBasedSplitBrainResolverProvider(system: ActorSystem) extends DowningProvider {

  val conf: Config = system.settings.config

  private val stableAfter = {
    val ttwDuration = Duration(conf.getString("akka.cluster.split-brain-resolver.stable-after"))
    FiniteDuration(ttwDuration.length, ttwDuration.unit)
  }

  private val coreRoles = conf.getStringList("akka.cluster.split-brain-resolver.essential-roles").asScala.toSet

  override def downRemovalMargin: FiniteDuration = stableAfter

  override def downingActorProps: Option[Props] = Some(RoleBasedSplitBrainResolver.props(stableAfter, coreRoles))
}
