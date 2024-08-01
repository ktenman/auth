package ee.tenman.auth.model

data class UserInfo(
    val email: String,
    val name: String,
    val givenName: String,
    val familyName: String,
    val picture: String
)
