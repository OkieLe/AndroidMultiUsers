package io.github.okiele.users

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.okiele.users.ui.theme.MultipleUsersTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var singleUserBinder: ISingleUser? = null

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "onServiceConnected $service")
            singleUserBinder = ISingleUser.Stub.asInterface(service)
            viewModel.setSameUser(singleUserBinder?.get(viewModel.currentUser) != false)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindService(
            Intent(ACTION).apply { `package` = packageName },
            connection, Context.BIND_AUTO_CREATE
        )
        setContent {
            val isSameUser = viewModel.getSameUser().observeAsState()
            val settings = viewModel.getSettings().observeAsState()
            MultipleUsersTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Content(
                        modifier = Modifier.fillMaxSize(),
                        currentUser = viewModel.currentUser,
                        isSameUser = isSameUser.value ?: true,
                        settings = settings.value.orEmpty(),
                        addSettings = { sameUser ->
                            val current = System.currentTimeMillis()
                            val secondInHour = current / TimeUnit.SECONDS.toMillis(1) % TimeUnit.HOURS.toSeconds(1)
                            Log.d(TAG, "Inserting $secondInHour $current")
                            if (sameUser) {
                                viewModel.sendSettings(singleUserBinder, secondInHour.toString(), current.toString())
                            } else {
                                viewModel.putSettings(secondInHour.toString(), current.toString())
                            }
                        },
                        broadcast = {
                            SingleUserReceiver.broadcast(this, viewModel.currentUser)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }

    companion object {

        private const val TAG = "MainActivity"
        private const val ACTION = "io.github.okiele.users.SINGLE_USER"
    }
}

@Composable
fun Content(
    modifier: Modifier = Modifier,
    currentUser: Int,
    isSameUser: Boolean = true,
    settings: List<String> = emptyList(),
    addSettings: (Boolean) -> Unit = {},
    broadcast: () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(3f)) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                text = "Application Current User: $currentUser",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                text = "Same User of Single User Service: $isSameUser",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.secondary
                )
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(settings) {
                    Text(text = it)
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                onClick = broadcast
            ) {
                Text(text = "Send Broadcast")
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                onClick = { addSettings(false) }
            ) {
                Text(text = "Add Settings(ContentResolver)")
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                onClick = { addSettings(true) }
            ) {
                Text(text = "Add Settings(AIDL -> DAO)")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MultipleUsersTheme {
        Content(Modifier.fillMaxSize(), currentUser = 0)
    }
}
