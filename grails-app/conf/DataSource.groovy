
hibernate {
  cache.use_second_level_cache = true
  cache.use_query_cache = true
  cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
}
// environment specific settings
dataSource {
  pooled = true
  //dialect = org.hibernate.dialect.Oracle10gDialect
  //username = "account"
  //password = "kj2mugp4ut8s"
  //logSql = false
  properties {
    maxActive = 25
    maxIdle = 10
    minIdle = 5
    initialSize = 5
    minEvictableIdleTimeMillis = 3600000
    timeBetweenEvictionRunsMillis = 3600000
    numTestsPerEvictionRun = 3
    testOnBorrow = false
    testWhileIdle = true
    testOnReturn = false
    validationQuery = "SELECT 1 from DUAL"
    maxWait = 10000
  }
}
