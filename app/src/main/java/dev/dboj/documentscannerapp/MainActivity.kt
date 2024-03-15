package dev.dboj.documentscannerapp

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dev.dboj.documentscannerapp.ui.theme.DocumentScannerAppTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            DocumentScannerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val imageUrisState = remember {
                        mutableStateListOf<Uri>()
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { activityResult ->
                            if (activityResult.resultCode == RESULT_OK) {
                                val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
                                imageUrisState.addAll(result?.pages?.map {
                                    it.imageUri
                                }?.toMutableStateList() ?: emptyList())

                                result?.pdf?.let { pdf ->
                                    val fileOutputStream = FileOutputStream(File(
                                        filesDir,
                                        "scan.pdf"
                                    ))

                                    contentResolver.openInputStream(pdf.uri)?.use {
                                        it.copyTo(fileOutputStream)
                                    }
                                }
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        imageUrisState.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = uri.toString(),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = {
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(it).build()
                                        )
                                    }
                                    .addOnFailureListener {e ->
                                        Toast.makeText(
                                                applicationContext,
                                                "Failed to start scanner: $e",
                                                Toast.LENGTH_LONG
                                            )
                                            .show()
                                    }
                            }) {
                                Text(text = R.string.scan.toString())
                            }
                        }
                    }


                }
            }
        }
    }
}

