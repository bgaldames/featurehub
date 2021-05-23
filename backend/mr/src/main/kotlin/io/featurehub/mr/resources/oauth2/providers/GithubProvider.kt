package io.featurehub.mr.resources.oauth2.providers

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.mr.resources.oauth2.AuthClientResult
import io.featurehub.mr.resources.oauth2.OAuth2JerseyClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class GithubProvider @Inject constructor(val client: Client) : OAuth2Provider {
  private val log: Logger = LoggerFactory.getLogger(GithubProvider::class.java)

  companion object {
    val PROVIDER_NAME = "oauth2-github"
  }

  @ConfigKey("oauth2.providers.github.secret")
  protected var oauthClientSecret: String? = null

  @ConfigKey("oauth2.providers.github.id")
  protected var oauthClientID: String? = null

  @ConfigKey("oauth2.redirectUrl")
  protected var redirectUrl: String? = null

  private val actualAuthUrl: String
  private val tokenUrl: String

  init {
    DeclaredConfigResolver.resolve(this)

    actualAuthUrl = "https://github.com/login/oauth/authorize?&scope=read:user%20user:email" +
      "&response_type=code&client_id=" + oauthClientID + "&redirect_uri=" +
      URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)

    tokenUrl = "https://github.com/login/oauth/access_token"
  }

  override fun discoverProviderUser(authed: AuthClientResult): ProviderUser? {
    val response: Response = client.target("https://api.github.com/user").request()
      .accept("application/vnd.github.v3+json")
      .header("Authorization", "Bearer " + authed.accessToken).get()

    if (response.statusInfo.family == Response.Status.Family.SUCCESSFUL) {
      val user = response.readEntity(GithubUser::class.java)

      if (user.email != null && user.name != null) {
        return ProviderUser.Builder().email(user.email).name(user.name).build()
      }

      log.warn("Failed to get user with name and email from Github", user)
    } else {
      log.warn("Failed when attempting to connect to Github for user details {}", response.status)
    }

    return null
  }

  override fun providerName(): String {
    return PROVIDER_NAME
  }

  override fun requestTokenUrl(): String {
    return tokenUrl
  }

  override fun requestAuthorizationUrl(): String {
    return actualAuthUrl
  }

  override fun getClientId(): String {
    return oauthClientID!!
  }

  override fun getClientSecret(): String {
    return oauthClientSecret!!
  }
}