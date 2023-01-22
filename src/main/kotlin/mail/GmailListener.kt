package mail

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Label
import java.io.*

class GmailListener {

    companion object {
        private val SCOPES: List<String> = listOf(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY)
        private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()

        private val CREDENTIALS_FILE_PATH = File(System.getProperty("user.dir")).resolve("credentials").resolve("gmail_client_secret.json")

        @Throws(IOException::class)
        private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential? {
            // Load client secrets.
            val `in`: InputStream = CREDENTIALS_FILE_PATH.inputStream()
//                ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
            val clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

            // Build flow and trigger user authorization request.
            val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
            )
                .setDataStoreFactory(FileDataStoreFactory(File("tokens")))
                .setAccessType("offline")
                .build()
            val receiver = LocalServerReceiver.Builder().setPort(8888).build()
            //returns an authorized Credential object.
            return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        }

    }

    fun setup(){

        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        val service = Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
            .setApplicationName("Oeuv Bot")
            .build()

        // Print the labels in the user's account.
        val user = "me"

        val msg = service.users().Messages().list(user).execute().messages
        msg.take(1).forEach {
            println(it)
            val id = it.id
            val m2 = service.users().messages().get(user, id).execute()

            println(m2.payload)
            m2.payload.parts.forEach { x ->
                println(String(x.body.decodeData()))
            }
            println(m2.payload.body.getSize())
            println(m2.payload.body.data)
        }

    }

}

fun main(){
    GmailListener().setup()
}