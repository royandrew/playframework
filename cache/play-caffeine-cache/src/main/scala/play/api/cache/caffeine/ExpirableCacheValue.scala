/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import akka.annotation.InternalApi

import scala.concurrent.duration.Duration

@InternalApi
private[caffeine] case class ExpirableCacheValue[V](value: V, durationMaybe: Option[Duration] = None)
