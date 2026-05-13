package com.inhoolee.locket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inhoolee.locket.data.AuthRepository
import com.inhoolee.locket.data.NotesRepository
import com.inhoolee.locket.data.SessionStore
import com.inhoolee.locket.data.SupabaseConfig
import com.inhoolee.locket.data.SupabaseHttpClient
import com.inhoolee.locket.ui.LocketApp
import com.inhoolee.locket.ui.LocketTheme
import com.inhoolee.locket.ui.LocketViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = SupabaseConfig(
            url = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY
        )
        val client = SupabaseHttpClient(config)
        val authRepository = AuthRepository(client, SessionStore(applicationContext))
        val notesRepository = NotesRepository(client, authRepository)
        val factory = LocketViewModelFactory(config, authRepository, notesRepository)

        setContent {
            LocketTheme {
                val viewModel: LocketViewModel = viewModel(factory = factory)
                LocketApp(viewModel = viewModel)
            }
        }
    }
}

private class LocketViewModelFactory(
    private val config: SupabaseConfig,
    private val authRepository: AuthRepository,
    private val notesRepository: NotesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LocketViewModel(config, authRepository, notesRepository) as T
    }
}
