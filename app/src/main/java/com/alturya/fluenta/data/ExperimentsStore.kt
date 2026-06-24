package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.alturya.fluenta.network.ApiClient
import kotlinx.coroutines.flow.first

// Local cache for A/B experiment variant assignments.
// Variants are fetched from /api/experiments/assign (sticky server-side) and
// cached locally so subsequent calls never need a network round-trip.
// A "control" variant = current baseline behavior; "treatment" = the new variant.
//
// Usage:
//   val variant = ExperimentsStore.getVariant(context, "onboarding_flow_v1")
//   if (variant == "treatment") { ... } else { ... }
//
// Experiment keys (keep in sync with backend experiment definitions):
object Experiments {
    const val ONBOARDING_FLOW_V1 = "onboarding_flow_v1"
    // Add new experiment keys here as you define them.
}

private val Context.experimentsDs by preferencesDataStore(name = "fluenta_experiments")

object ExperimentsStore {

    // Returns the variant for the given experiment key, fetching from the server
    // on first call. Returns "control" on any error so the baseline is safe.
    suspend fun getVariant(context: Context, experimentKey: String): String {
        val prefKey = stringPreferencesKey("exp_$experimentKey")
        val prefs = context.experimentsDs.data.first()
        val cached = prefs[prefKey]
        if (cached != null) return cached

        return try {
            val anonId = TokenStore.getOrCreateAnonId(context)
            val response = ApiClient.api.getExperimentVariant(key = experimentKey, anonId = anonId)
            val variant = response.variant
            context.experimentsDs.edit { it[prefKey] = variant }
            // Log exposure for analysis
            Analytics.track(
                context,
                Analytics.EXPERIMENT_EXPOSURE,
                mapOf("experiment" to experimentKey, "variant" to variant),
            )
            variant
        } catch (_: Exception) {
            "control"
        }
    }

    // Force-reset a cached assignment (e.g., after QA testing).
    suspend fun reset(context: Context, experimentKey: String) {
        val prefKey = stringPreferencesKey("exp_$experimentKey")
        context.experimentsDs.edit { it.remove(prefKey) }
    }
}
