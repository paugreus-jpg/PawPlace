package com.example.dogmap.ui

import androidx.compose.runtime.compositionLocalOf
import com.example.dogmap.viewmodel.DogViewModelFactory

val LocalViewModelFactory = compositionLocalOf<DogViewModelFactory> {
    error("ViewModelFactory not provided")
}
