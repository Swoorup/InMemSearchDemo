package inmemdb

object common {
  extension [K, V](m1: Map[K, V]) {

    infix def merge(m2: Map[K, V])(duplicateResolve: (v1: V, v2: V) => V): Map[K, V] = {
      m1 ++ m2.map { case (k, v2) =>
        val resolvedValue = m1.get(k) match
          case Some(v1) => duplicateResolve(v1, v2)
          case None     => v2
        k -> resolvedValue
      }
    }

  }
}
