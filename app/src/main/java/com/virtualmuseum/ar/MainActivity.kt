package com.virtualmuseum.ar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.ar.core.Config
import com.virtualmuseum.ar.theme.ARMenuTheme
import com.virtualmuseum.ar.theme.Blue500
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARMenuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()){
                        val currentModel = remember {
                            mutableStateOf("aphrodite")
                        }
                        ARScreen(currentModel.value)
                        Menu(modifier = Modifier.align(Alignment.BottomCenter)
                        ){
                            currentModel.value = it
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Menu(modifier: Modifier, onClick:(String)->Unit) {
    var currentIndex by remember {
        mutableStateOf(0)
    }

    val itemsList = listOf(
        Statue("aphrodite", "Aphrodite"),
        Statue("doraemon", "Doraemon"),
        Statue("merlion", "Merlion"),
        Statue("stone_vase", "Stone Vase"),
        Statue("stone", "Stone Moai"),
    )

    fun updateIndex(offset:Int){
        currentIndex = (currentIndex+offset + itemsList.size) % itemsList.size
        onClick(itemsList[currentIndex].name)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.background.copy(alpha = 0.8f))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { updateIndex(-1) },
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.circle_arrow_left),
                    contentDescription = "previous",
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = itemsList[currentIndex].displayText,
                style = MaterialTheme.typography.subtitle1,
            )

            IconButton(
                onClick = { updateIndex(1) },
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.circle_arrow_right),
                    contentDescription = "next",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ARScreen(model: String) {
    val nodes = remember {
        mutableListOf<ArNode>()
    }
    val modelNode = remember {
        mutableStateOf<ArModelNode?>(null)
    }
    val placeModelButton = remember {
        mutableStateOf(false)
    }
    val isModelPlaced = remember {
        mutableStateOf(false)
    }
    val currentTrackedModel = remember {
        mutableStateOf("")
    }

    var modelScale by remember { mutableStateOf(0.8f) }
    var modelRotationY by remember { mutableStateOf(0f) }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, rotation ->
                if (isModelPlaced.value) {
                    modelScale *= zoom
                    modelScale = modelScale.coerceIn(0.1f, 2.0f)
                    modelNode.value?.scale = Scale(modelScale)

                    modelRotationY += rotation
                    modelNode.value?.rotation = Rotation(0f, modelRotationY, 0f)

                    val currentPosition = modelNode.value?.position ?: Position(0f, 0f, 0f)
                    modelNode.value?.position = Position(
                        currentPosition.x + pan.x * 0.01f,
                        currentPosition.y,
                        currentPosition.z + pan.y * 0.01f
                    )
                }
            }
        }
    ) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            planeRenderer = true,
            onCreate = { arSceneView ->
                arSceneView.lightEstimationMode = Config.LightEstimationMode.DISABLED
                arSceneView.planeRenderer.isShadowReceiver = false
                modelNode.value = ArModelNode(arSceneView.engine, PlacementMode.INSTANT).apply {
                    loadModelGlbAsync(
                        glbFileLocation = "models/${model}.glb",
                        scaleToUnits = modelScale
                    ) {
                        currentTrackedModel.value = model
                    }

                    onAnchorChanged = {
                        placeModelButton.value = !isAnchored
                        isModelPlaced.value = isAnchored
                    }

                    onHitResult = { node, hitResult ->
                        if (node.isTracking && !isModelPlaced.value) {
                            placeModelButton.value = true
                        }
                    }
                }
                nodes.add(modelNode.value!!)
            },
            onSessionCreate = {
                planeRenderer.isVisible = true
            }
        )

        if (placeModelButton.value) {
            Button(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Blue500,
                    contentColor = Color.White
                ),
                onClick = {
                    modelNode.value?.anchor()
                    placeModelButton.value = false
                    isModelPlaced.value = true
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 180.dp)
            ) {
                Text(text = "Place It")
            }
        }

        if (isModelPlaced.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Blue500,
                        contentColor = Color.White
                    ),
                    onClick = {
                        modelScale = (modelScale * 1.2f).coerceAtMost(2.0f)
                        modelNode.value?.scale = Scale(modelScale)
                }) {
                    Text(text = "Scale +")
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Blue500,
                        contentColor = Color.White
                    ),
                    onClick = {
                        modelScale = (modelScale * 0.8f).coerceAtLeast(0.1f)
                        modelNode.value?.scale = Scale(modelScale)
                }) {
                    Text(text = "Scale -")
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Blue500,
                        contentColor = Color.White
                    ),
                    onClick = {
                        modelRotationY += 45f
                        modelNode.value?.rotation = Rotation(0f, modelRotationY, 0f)
                }) {
                    Text(text = "Rotate")
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Blue500,
                        contentColor = Color.White
                    ),
                    onClick = {
                        modelNode.value?.detachAnchor()
                        isModelPlaced.value = false
                        placeModelButton.value = true
                }) {
                    Text(text = "Reset")
                }
            }
        }
    }

    LaunchedEffect(key1 = model) {
        modelNode.value?.detachAnchor()
        placeModelButton.value = false
        isModelPlaced.value = false
        modelScale = 0.8f
        modelRotationY = 0f

        modelNode.value?.loadModelGlbAsync(
            glbFileLocation = "models/${model}.glb",
            scaleToUnits = modelScale
        )

        currentTrackedModel.value = model
    }
}

data class Statue(var name: String, var displayText: String)