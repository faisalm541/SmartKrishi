package com.example.smartkrishi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class DiseaseRepository {

    suspend fun detectDisease(
        context: Context,
        imageUri: Uri
    ): Result<DiseaseResponse> {
        return try {

            // Compress image before sending — prevents timeout on large camera photos
            val bytes = compressImage(context, imageUri)
                ?: return Result.failure(Exception("Could not read image file"))

            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                name     = "image",
                filename = "crop_image.jpg",
                body     = requestBody
            )

            val response = DiseaseApi.service.detectDisease(part)

            // ── Handle all response cases from your Flask API ─
            when {

                // Case 1: API explicitly says not_recognized
                // Flask returns: { "status": "not_recognized", "confidence": 0.3, "message": "..." }
                response.status == "not_recognized" -> {
                    Result.failure(NotRecognizedException())
                }

                // Case 2: API says error
                // Flask returns: { "status": "error", "message": "..." }
                response.status == "error" -> {
                    Result.failure(ServerException(500))
                }

                // Case 3: Successful prediction — no status field
                // Flask returns: { "crop": "Tomato", "disease": "Septoria", "solution": "...", "confidence": 0.96 }
                !response.crop.isNullOrBlank() && !response.disease.isNullOrBlank() -> {
                    Result.success(response)
                }

                // Case 4: Unexpected empty response
                else -> {
                    Result.failure(NotRecognizedException())
                }
            }

        } catch (e: NotRecognizedException) {
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(NoInternetException())
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(TimeoutException())
        } catch (e: retrofit2.HttpException) {
            Result.failure(ServerException(e.code()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Compress to max 800×800 JPEG — prevents timeout ──────
    private fun compressImage(context: Context, uri: Uri): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val maxDim = 800
            var sampleSize = 1
            while (options.outWidth  / sampleSize > maxDim * 2 ||
                   options.outHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            val scaledBitmap = scaleBitmap(rawBitmap, maxDim)
            val output = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)

            if (scaledBitmap != rawBitmap) rawBitmap.recycle()
            scaledBitmap.recycle()

            output.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width  = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        return Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(),
            (height * ratio).toInt(), true)
    }
}

// ── Custom exceptions ─────────────────────────────────────────
class NotRecognizedException : Exception("Image not recognized")
class NoInternetException    : Exception("No internet connection")
class TimeoutException       : Exception("Request timed out")
class ServerException(val code: Int) : Exception("Server error $code")
