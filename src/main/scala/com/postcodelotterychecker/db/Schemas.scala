package com.postcodelotterychecker.db

sealed trait Schema {
  val tableName: String
  val primaryKey: List[String]
}
case class SubscriberSchema(
                                  tableName: String = "subscribers",
                                  userId: String = "user_id",
                                  email: String = "email",
                                  notifyWhen: String = "notify_when",
                                  postcodesWatching: String = "postcodes_watching",
                                  dinnerUsersWatching: String = "dinner_users_watching",
                                  emojisWatching: String = "emojis_watching",
                                  lastUpdated: String = "last_updated") extends Schema {
  override val primaryKey: List[String] = List(userId)
}