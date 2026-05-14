package com.example.unisphere.db

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

object SupabaseClient {
    // Inizializza il client una sola volta per tutta l'app
    val client = createSupabaseClient(
        supabaseUrl = "https://zxbaanmhucatksafgdhu.supabase.co",
        supabaseKey = "sb_publishable_Uo5uPyF7PYnPgu1iogO-Qw_McH6beWD"
    ) {
        install(Auth)
    }
}