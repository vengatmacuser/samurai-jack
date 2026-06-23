package com.thigazhini_labs.samuraijack.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.thigazhini_labs.samuraijack.models.Mesh
import com.thigazhini_labs.samuraijack.stages.Stages
import java.util.Collections
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val context: android.content.Context) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "GLRenderer"
    }

    // Texture loading helper
    private val textureCache = mutableMapOf<Int, Int>()

    private fun loadTexture(resourceId: Int): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) {
            return 0
        }

        val options = android.graphics.BitmapFactory.Options().apply {
            inScaled = false
        }
        val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resourceId, options) ?: return 0

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
        
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        android.opengl.GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return textureIds[0]
    }

    private fun getTexture(resId: Int): Int {
        var tex = textureCache[resId]
        if (tex == null || tex == 0) {
            tex = loadTexture(resId)
            if (tex != 0) {
                textureCache[resId] = tex
            }
        }
        return tex ?: 0
    }

    private val assetTextureCache = mutableMapOf<String, Int>()

    private fun loadAssetTexture(fileName: String): Int {
        var tex = assetTextureCache[fileName]
        if (tex != null && tex != 0) return tex

        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return 0

        try {
            context.assets.open(fileName).use { inputStream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) return 0
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
                android.opengl.GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                bitmap.recycle()
                assetTextureCache[fileName] = textureIds[0]
                return textureIds[0]
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading asset texture: $fileName", e)
            return 0
        }
    }

    private fun getStageBackgroundResId(stageIndex: Int): Int {
        return when (stageIndex) {
            0 -> com.thigazhini_labs.samuraijack.R.drawable.bg_frosthollow
            1 -> com.thigazhini_labs.samuraijack.R.drawable.bg_forest
            2 -> com.thigazhini_labs.samuraijack.R.drawable.bg_jungle
            3 -> com.thigazhini_labs.samuraijack.R.drawable.bg_village
            4 -> com.thigazhini_labs.samuraijack.R.drawable.bg_port
            5 -> com.thigazhini_labs.samuraijack.R.drawable.bg_port
            6 -> com.thigazhini_labs.samuraijack.R.drawable.bg_port
            7 -> com.thigazhini_labs.samuraijack.R.drawable.bg_port
            8 -> com.thigazhini_labs.samuraijack.R.drawable.bg_forest
            9 -> com.thigazhini_labs.samuraijack.R.drawable.bg_jungle
            10 -> com.thigazhini_labs.samuraijack.R.drawable.bg_desert
            11 -> com.thigazhini_labs.samuraijack.R.drawable.bg_desert
            12 -> com.thigazhini_labs.samuraijack.R.drawable.bg_village
            else -> com.thigazhini_labs.samuraijack.R.drawable.bg_village
        }
    }

    // Shader handles
    private var programHandle = 0
    private var mvpMatrixLink = 0
    private var modelMatrixLink = 0
    private var cameraPosLink = 0
    private var fogColorLink = 0
    private var fogDensityLink = 0
    private var dirLightDirLink = 0
    private var dirLightColorLink = 0
    private var pointLightPosLink = 0
    private var pointLightColorLink = 0
    private var pointLightIntensityLink = 0
    private var silhouetteModeLink = 0
    private var hitFlashRedLink = 0

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Camera stats
    var cameraPos = Vector3(0f, 2.2f, -5f)
    var cameraTarget = Vector3(0f, 0.6f, 0f)

    // Threat level parameters
    var hitFlashAmount = 0.0f
    var currentStageIndex = 0

    // Thread-safe list of active game meshes
    val renderMeshes: MutableList<Mesh> = Collections.synchronizedList(mutableListOf())

    // Point light animation parameter (e.g. from laser blasts)
    var pointLightPos = Vector3(0f, 10f, 0f)
    var pointLightColor = floatArrayOf(1f, 0.1f, 0.1f)
    var pointLightIntensity = 0.0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Enable depth testing
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)

        // Compile and link shaders
        val vertexShader = Shader.loadShader(GLES30.GL_VERTEX_SHADER, Shader.VERTEX_SHADER_CODE)
        val fragmentShader = Shader.loadShader(GLES30.GL_FRAGMENT_SHADER, Shader.FRAGMENT_SHADER_CODE)
        programHandle = Shader.linkProgram(vertexShader, fragmentShader)

        if (programHandle != 0) {
            // Get uniform links
            mvpMatrixLink = GLES30.glGetUniformLocation(programHandle, "uMVPMatrix")
            modelMatrixLink = GLES30.glGetUniformLocation(programHandle, "uModelMatrix")
            cameraPosLink = GLES30.glGetUniformLocation(programHandle, "uCameraPos")
            fogColorLink = GLES30.glGetUniformLocation(programHandle, "uFogColor")
            fogDensityLink = GLES30.glGetUniformLocation(programHandle, "uFogDensity")
            dirLightDirLink = GLES30.glGetUniformLocation(programHandle, "uDirLightDir")
            dirLightColorLink = GLES30.glGetUniformLocation(programHandle, "uDirLightColor")
            pointLightPosLink = GLES30.glGetUniformLocation(programHandle, "uPointLightPos")
            pointLightColorLink = GLES30.glGetUniformLocation(programHandle, "uPointLightColor")
            pointLightIntensityLink = GLES30.glGetUniformLocation(programHandle, "uPointLightIntensity")
            silhouetteModeLink = GLES30.glGetUniformLocation(programHandle, "uSilhouetteMode")
            hitFlashRedLink = GLES30.glGetUniformLocation(programHandle, "uHitFlashRed")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        
        // Perspective projection: 60-degree field of view
        Matrix.perspectiveM(projectionMatrix, 0, 50.0f, ratio, 0.1f, 150.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val stage = Stages.stagesList.getOrNull(currentStageIndex) ?: Stages.stagesList[0]

        // 1. Clear background to be fully transparent so the dynamic 2D background image shows through
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (programHandle == 0) return

        GLES30.glUseProgram(programHandle)

        // 2. Configure View Matrix (Camera LookAt position)
        Matrix.setLookAtM(
            viewMatrix, 0,
            cameraPos.x, cameraPos.y, cameraPos.z,
            cameraTarget.x, cameraTarget.y, cameraTarget.z,
            0f, 1f, 0f
        )

        // 3. Upload Global Uniforms
        GLES30.glUniform3f(cameraPosLink, cameraPos.x, cameraPos.y, cameraPos.z)
        GLES30.glUniform3f(fogColorLink, stage.fogColor[0], stage.fogColor[1], stage.fogColor[2])
        GLES30.glUniform1f(fogDensityLink, stage.fogDensity)

        // Directional Sunlight (Steeper angle for dramatic side-shadows)
        GLES30.glUniform3f(dirLightDirLink, -1.2f, -0.8f, 0.4f)
        GLES30.glUniform3f(dirLightColorLink, 1.0f, 0.95f, 0.90f)

        // Point light (pulsing lasers / sword impact glow)
        GLES30.glUniform3f(pointLightPosLink, pointLightPos.x, pointLightPos.y, pointLightPos.z)
        GLES30.glUniform3f(pointLightColorLink, pointLightColor[0], pointLightColor[1], pointLightColor[2])
        GLES30.glUniform1f(pointLightIntensityLink, pointLightIntensity)

        // Red Hit flash uniform
        GLES30.glUniform1f(hitFlashRedLink, hitFlashAmount)

        // 4. Render Meshes
        synchronized(renderMeshes) {
            for (mesh in renderMeshes) {
                if (!mesh.isVisible) continue

                // Model matrix transformations
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, mesh.position.x, mesh.position.y, mesh.position.z)
                
                // Rotation (Y, then X, then Z)
                if (mesh.rotation.y != 0f) {
                    Matrix.rotateM(modelMatrix, 0, mesh.rotation.y, 0f, 1f, 0f)
                }
                if (mesh.rotation.x != 0f) {
                    Matrix.rotateM(modelMatrix, 0, mesh.rotation.x, 1f, 0f, 0f)
                }
                if (mesh.rotation.z != 0f) {
                    Matrix.rotateM(modelMatrix, 0, mesh.rotation.z, 0f, 0f, 1f)
                }
                
                Matrix.scaleM(modelMatrix, 0, mesh.scale.x, mesh.scale.y, mesh.scale.z)

                // Calculate MVP
                Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
                val finalMVP = FloatArray(16)
                Matrix.multiplyMM(finalMVP, 0, projectionMatrix, 0, mvpMatrix, 0)

                // Upload uniform handles
                GLES30.glUniformMatrix4fv(mvpMatrixLink, 1, false, finalMVP, 0)
                GLES30.glUniformMatrix4fv(modelMatrixLink, 1, false, modelMatrix, 0)
                
                // Silhouette rendering modes (0 = normal, 1 = black body, 2 = glowing parts)
                GLES30.glUniform1i(silhouetteModeLink, mesh.silhouetteMode)

                // Bind texture if in textured mode
                val useTexture = (mesh.silhouetteMode == 3 || mesh.silhouetteMode == 4) && mesh.texCoordBuffer != null
                if (useTexture) {
                    val texId = if (mesh.silhouetteMode == 4 && mesh.textureName != null) {
                        loadAssetTexture(mesh.textureName!!)
                    } else {
                        val texResId = getStageBackgroundResId(currentStageIndex)
                        getTexture(texResId)
                    }
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
                    val uTextureLoc = GLES30.glGetUniformLocation(programHandle, "uTexture")
                    GLES30.glUniform1i(uTextureLoc, 0)

                    mesh.texCoordBuffer!!.position(0)
                    GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, 0, mesh.texCoordBuffer)
                    GLES30.glEnableVertexAttribArray(3)
                }

                // Bind Vertex Data
                mesh.vertexBuffer.position(0)
                GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, mesh.vertexBuffer)
                GLES30.glEnableVertexAttribArray(0)

                // Bind Normals
                mesh.normalBuffer.position(0)
                GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, mesh.normalBuffer)
                GLES30.glEnableVertexAttribArray(1)

                // Bind Colors
                mesh.colorBuffer.position(0)
                GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 0, mesh.colorBuffer)
                GLES30.glEnableVertexAttribArray(2)

                // Draw Elements
                mesh.indexBuffer.position(0)
                GLES30.glDrawElements(
                    GLES30.GL_TRIANGLES,
                    mesh.indexCount,
                    GLES30.GL_UNSIGNED_SHORT,
                    mesh.indexBuffer
                )

                // Disable arrays
                GLES30.glDisableVertexAttribArray(0)
                GLES30.glDisableVertexAttribArray(1)
                GLES30.glDisableVertexAttribArray(2)
                if (useTexture) {
                    GLES30.glDisableVertexAttribArray(3)
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
                }
            }
        }
    }
}
