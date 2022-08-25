// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.jan.supacompose.auth.Auth
import io.github.jan.supacompose.auth.auth
import io.github.jan.supacompose.auth.providers.Email
import io.github.jan.supacompose.auth.providers.Google
import io.github.jan.supacompose.auth.sessionFile
import io.github.jan.supacompose.createSupabaseClient
import io.github.jan.supacompose.postgrest.Postgrest
import io.github.jan.supacompose.postgrest.postgrest
import io.github.jan.supacompose.realtime.events.ChannelAction
import io.github.jan.supacompose.realtime.Realtime
import io.github.jan.supacompose.realtime.createAndJoinChannel
import io.github.jan.supacompose.realtime.createChannel
import io.github.jan.supacompose.realtime.realtime
import io.github.jan.supacompose.storage.Storage
import io.github.jan.supacompose.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class User(val id: String, val username: String)

suspend fun main() {
    val client = createSupabaseClient {
        supabaseUrl = System.getenv("SUPABASE_URL")
        supabaseKey = System.getenv("SUPABASE_KEY")

        install(Auth) {
            sessionFile = File("C:\\Users\\jan\\AppData\\Local\\SupaCompose\\usersession.json")
        }
        install(Realtime)
        install(Postgrest)
        install(Storage)
    }
    val scope = CoroutineScope(Dispatchers.IO)
    application {
        Window(::exitApplication) {
            val session by client.auth.currentSession.collectAsState()
            val status by client.realtime.status.collectAsState()
            println(session?.accessToken)
            if (session != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Logged in as ${session?.user?.email}")
                }
                LaunchedEffect(Unit) {
                    scope.launch {

                    }
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    Column {
                        TextField(email, { email = it }, placeholder = { Text("Email") })
                        TextField(
                            password,
                            { password = it },
                            placeholder = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(onClick = {
                            scope.launch {
                                client.auth.loginWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                            }
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Login")
                        }
                        Button(onClick = {
                            scope.launch {
                                client.auth.loginWith(Google)
                            }
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Login with Discord")
                        }
                        //
                    }
                }

            }
        }
    }

}

/**fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}*/
