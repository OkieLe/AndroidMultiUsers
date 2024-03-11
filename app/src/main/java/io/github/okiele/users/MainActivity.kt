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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.MutableLiveData
import io.github.okiele.users.ui.theme.MultipleUsersTheme

class MainActivity : ComponentActivity() {

    private val userGetter = {
        applicationContext.getUser().getIdentifier()
    }
    private val sameUserData = MutableLiveData(true)

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "onServiceConnected $service")
            sameUserData.postValue(ISingleUser.Stub.asInterface(service)?.get(userGetter()) != false)
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
            val isSameUser = sameUserData.observeAsState()
            MultipleUsersTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Content(Modifier.fillMaxSize(), userGetter = userGetter, isSameUser = isSameUser.value ?: true)
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
fun Content(modifier: Modifier = Modifier, userGetter: () -> Int, isSameUser: Boolean = true) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp),
            text = "Application Current User: ${userGetter()}",
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
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MultipleUsersTheme {
        Content(Modifier.fillMaxSize(), userGetter = { 0 })
    }
}
