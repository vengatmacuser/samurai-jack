package com.thigazhini_labs.samuraijack.models

import com.thigazhini_labs.samuraijack.engine.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Mesh {
    lateinit var vertexBuffer: FloatBuffer
    lateinit var normalBuffer: FloatBuffer
    lateinit var colorBuffer: FloatBuffer
    lateinit var indexBuffer: ShortBuffer
    var texCoordBuffer: FloatBuffer? = null
    var indexCount: Int = 0

    var position = Vector3(0f, 0f, 0f)
    var rotation = Vector3(0f, 0f, 0f)
    var scale = Vector3(1f, 1f, 1f)
    var isVisible = true
    var silhouetteMode = 0 // 0=normal, 1=silhouette, 2=glow
    var textureName: String? = null
    var isHero = false

    fun initBuffers(vertices: FloatArray, normals: FloatArray, colors: FloatArray, indices: ShortArray) {
        indexCount = indices.size

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(vertices)
                position(0)
            }

        normalBuffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(normals)
                position(0)
            }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(colors)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                put(indices)
                position(0)
            }
    }

}

object Models3D {

    class MeshBuilder {
        private val vertices = mutableListOf<Float>()
        private val normals = mutableListOf<Float>()
        private val colors = mutableListOf<Float>()
        private val indices = mutableListOf<Short>()
        private var vertexCount = 0

        fun appendBox(
            x: Float, y: Float, z: Float,
            w: Float, h: Float, d: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f
        ) {
            val halfW = w / 2f
            val halfH = h / 2f
            val halfD = d / 2f

            val facePositions = arrayOf(
                // Front
                floatArrayOf(x - halfW, y - halfH, z + halfD,  x + halfW, y - halfH, z + halfD,  x + halfW, y + halfH, z + halfD,  x - halfW, y + halfH, z + halfD),
                // Back
                floatArrayOf(x - halfW, y - halfH, z - halfD,  x - halfW, y + halfH, z - halfD,  x + halfW, y + halfH, z - halfD,  x + halfW, y - halfH, z - halfD),
                // Left
                floatArrayOf(x - halfW, y - halfH, z - halfD,  x - halfW, y - halfH, z + halfD,  x - halfW, y + halfH, z + halfD,  x - halfW, y + halfH, z - halfD),
                // Right
                floatArrayOf(x + halfW, y - halfH, z - halfD,  x + halfW, y + halfH, z - halfD,  x + halfW, y + halfH, z + halfD,  x + halfW, y - halfH, z + halfD),
                // Top
                floatArrayOf(x - halfW, y + halfH, z - halfD,  x - halfW, y + halfH, z + halfD,  x + halfW, y + halfH, z + halfD,  x + halfW, y + halfH, z - halfD),
                // Bottom
                floatArrayOf(x - halfW, y - halfH, z - halfD,  x + halfW, y - halfH, z - halfD,  x + halfW, y - halfH, z + halfD,  x - halfW, y - halfH, z + halfD)
            )

            val faceNormals = arrayOf(
                floatArrayOf(0f, 0f, 1f),
                floatArrayOf(0f, 0f, -1f),
                floatArrayOf(-1f, 0f, 0f),
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, -1f, 0f)
            )

            for (i in 0..5) {
                val startV = vertexCount.toShort()
                val pos = facePositions[i]
                val norm = faceNormals[i]

                for (v in 0..3) {
                    vertices.add(pos[v * 3])
                    vertices.add(pos[v * 3 + 1])
                    vertices.add(pos[v * 3 + 2])

                    normals.add(norm[0])
                    normals.add(norm[1])
                    normals.add(norm[2])

                    colors.add(r)
                    colors.add(g)
                    colors.add(b)
                    colors.add(a)
                }

                indices.add(startV)
                indices.add((startV + 1).toShort())
                indices.add((startV + 2).toShort())

                indices.add(startV)
                indices.add((startV + 2).toShort())
                indices.add((startV + 3).toShort())

                vertexCount += 4
            }
        }

        private fun MutableList<Float>.add(vararg elements: Float) {
            for (e in elements) this.add(e)
        }

        fun build(): Mesh {
            val mesh = Mesh()
            mesh.initBuffers(
                vertices.toFloatArray(),
                normals.toFloatArray(),
                colors.toFloatArray(),
                indices.toShortArray()
            )
            return mesh
        }
    }

    fun createGround(width: Float, depth: Float, r: Float, g: Float, b: Float): Mesh {
        val mb = MeshBuilder()
        // Prince of Persia Style detailed stone bridge with side railings
        // Main road platform
        mb.appendBox(0f, -0.5f, 0f, width, 0.4f, depth, 0.22f, 0.22f, 0.24f)
        
        // Stone Railing posts/parapets spaced out along sides
        val halfW = width / 2f
        val step = 4.0f
        var z = -depth / 2f
        while (z < depth / 2f) {
            // Left battlements
            mb.appendBox(-halfW + 0.15f, 0f, z + 1f, 0.3f, 0.6f, 2.0f, 0.3f, 0.3f, 0.32f)
            // Right battlements
            mb.appendBox(halfW - 0.15f, 0f, z + 1f, 0.3f, 0.6f, 2.0f, 0.3f, 0.3f, 0.32f)
            z += step
        }
        return mb.build()
    }

    fun createToriiGate(): Mesh {
        val mb = MeshBuilder()
        val woodR = 0.38f
        val woodG = 0.17f
        val woodB = 0.08f
        val postX = 1.25f
        mb.appendBox(-postX, 1.6f, 0f, 0.3f, 3.2f, 0.3f, woodR, woodG, woodB)
        mb.appendBox(postX, 1.6f, 0f, 0.3f, 3.2f, 0.3f, woodR, woodG, woodB)
        mb.appendBox(0f, 3.3f, 0f, 3.6f, 0.22f, 0.4f, 0.35f, 0.15f, 0.08f)
        mb.appendBox(0f, 1.9f, 0f, 2.8f, 0.12f, 0.28f, 0.3f, 0.14f, 0.07f)
        mb.appendBox(0f, 1.2f, 0f, 0.22f, 0.16f, 0.18f, 0.32f, 0.16f, 0.08f)
        mb.appendBox(-postX, 0.08f, 0f, 0.46f, 0.16f, 0.46f, 0.3f, 0.14f, 0.07f)
        mb.appendBox(postX, 0.08f, 0f, 0.46f, 0.16f, 0.46f, 0.3f, 0.14f, 0.07f)
        return mb.build()
    }

    fun createSkySphere(): Mesh {
        val mesh = Mesh()
        val radius = 80f
        val rings = 16
        val sectors = 24

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()

        for (rIdx in 0 until rings) {
            val phi = Math.PI * rIdx * R
            val sinPhi = Math.sin(phi)
            val cosPhi = Math.cos(phi)

            for (sIdx in 0 until sectors) {
                val theta = 2.0 * Math.PI * sIdx * S
                val sinTheta = Math.sin(theta)
                val cosTheta = Math.cos(theta)

                val vx = (radius * sinTheta * sinPhi).toFloat()
                val vy = (radius * cosPhi).toFloat()
                val vz = (radius * cosTheta * sinPhi).toFloat()

                vertices.add(vx)
                vertices.add(vy)
                vertices.add(vz)

                val len = kotlin.math.sqrt(vx * vx + vy * vy + vz * vz)
                normals.add(-vx / len)
                normals.add(-vy / len)
                normals.add(-vz / len)

                colors.add(1f)
                colors.add(1f)
                colors.add(1f)
                colors.add(1f)

                texCoords.add(sIdx * S)
                texCoords.add(rIdx * R)
            }
        }

        for (rIdx in 0 until rings - 1) {
            for (sIdx in 0 until sectors - 1) {
                val k1 = rIdx * sectors + sIdx
                val k2 = k1 + sectors
                indices.add(k1.toShort())
                indices.add(k2.toShort())
                indices.add((k1 + 1).toShort())
                indices.add((k1 + 1).toShort())
                indices.add(k2.toShort())
                indices.add((k2 + 1).toShort())
            }
        }

        mesh.initBuffers(
            vertices.toFloatArray(),
            normals.toFloatArray(),
            colors.toFloatArray(),
            indices.toShortArray()
        )

        mesh.texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(texCoords.toFloatArray())
                position(0)
            }
        mesh.silhouetteMode = 3
        return mesh
    }

    fun loadObj(
        context: android.content.Context,
        objFileName: String,
        mtlFileName: String?,
        scaleMultiplier: Float = 1.0f,
        rotationOffsetY: Float = 180f,
        yOffset: Float = -0.265f
    ): Mesh {
        val materials = mutableMapOf<String, FloatArray>()
        var currentMaterial: String? = null
        var textureFileName: String? = null

        // 1. Read MTL file if present
        if (!mtlFileName.isNullOrEmpty()) {
            try {
                context.assets.open(mtlFileName).bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("newmtl ")) {
                            currentMaterial = trimmed.substring("newmtl ".length).trim()
                        } else if (trimmed.startsWith("Kd ") && currentMaterial != null) {
                            val parts = trimmed.substring("Kd ".length).trim().split("\\s+".toRegex())
                            if (parts.size >= 3) {
                                val r = parts[0].toFloatOrNull() ?: 1.0f
                                val g = parts[1].toFloatOrNull() ?: 1.0f
                                val b = parts[2].toFloatOrNull() ?: 1.0f
                                materials[currentMaterial!!] = floatArrayOf(r, g, b, 1.0f)
                            }
                        } else if (trimmed.startsWith("map_Kd ")) {
                            textureFileName = trimmed.substring("map_Kd ".length).trim()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Models3D", "Error reading mtl file: $mtlFileName", e)
            }
        }

        // 2. Read OBJ file
        val rawPositions = mutableListOf<Vector3>()
        val rawNormals = mutableListOf<Vector3>()
        val rawTexCoords = mutableListOf<FloatArray>()
        val rawColors = mutableListOf<FloatArray>()

        val uniqueVertices = mutableMapOf<String, Short>()
        val outVertices = mutableListOf<Float>()
        val outNormals = mutableListOf<Float>()
        val outColors = mutableListOf<Float>()
        val outTexCoords = mutableListOf<Float>()
        val outIndices = mutableListOf<Short>()

        fun getOrCreateVertex(token: String, currentColor: FloatArray): Short {
            val parts = token.split("/")
            val vIdx = parts[0].toIntOrNull() ?: 1
            val vtIdx = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].toIntOrNull() ?: -1 else -1
            val vnIdx = if (parts.size > 2 && parts[2].isNotEmpty()) parts[2].toIntOrNull() ?: -1 else -1

            // Check if this vertex has an embedded color (from v line)
            val vColor = rawColors.getOrNull(vIdx - 1)
            val finalColor = if (vColor != null && vColor[0] >= 0f) {
                vColor
            } else {
                currentColor
            }

            val key = "$token|${finalColor[0]},${finalColor[1]},${finalColor[2]}"
            uniqueVertices[key]?.let { return it }

            val pos = rawPositions.getOrElse(vIdx - 1) { Vector3(0f, 0f, 0f) }
            val norm = if (vnIdx != -1) rawNormals.getOrElse(vnIdx - 1) { Vector3(0f, 1f, 0f) } else Vector3(0f, 1f, 0f)
            val tex = if (vtIdx != -1) rawTexCoords.getOrElse(vtIdx - 1) { floatArrayOf(0f, 0f) } else floatArrayOf(0f, 0f)

            // Offset & Scale vertex position
            val adjustedX = pos.x * scaleMultiplier
            val adjustedY = pos.y * scaleMultiplier + yOffset
            val adjustedZ = pos.z * scaleMultiplier

            // Apply rotationOffsetY around Y axis if any
            var finalX = adjustedX
            var finalZ = adjustedZ
            if (rotationOffsetY != 0f) {
                val rad = Math.toRadians(rotationOffsetY.toDouble())
                val cos = Math.cos(rad).toFloat()
                val sin = Math.sin(rad).toFloat()
                finalX = adjustedX * cos - adjustedZ * sin
                finalZ = adjustedX * sin + adjustedZ * cos
            }

            // Apply rotationOffsetY around Y axis to normals if any
            var finalNx = norm.x
            var finalNz = norm.z
            if (rotationOffsetY != 0f) {
                val rad = Math.toRadians(rotationOffsetY.toDouble())
                val cos = Math.cos(rad).toFloat()
                val sin = Math.sin(rad).toFloat()
                finalNx = norm.x * cos - norm.z * sin
                finalNz = norm.x * sin + norm.z * cos
            }

            outVertices.add(finalX)
            outVertices.add(adjustedY)
            outVertices.add(finalZ)

            outNormals.add(finalNx)
            outNormals.add(norm.y)
            outNormals.add(finalNz)

            outColors.add(finalColor[0])
            outColors.add(finalColor[1])
            outColors.add(finalColor[2])
            outColors.add(finalColor[3])

            outTexCoords.add(tex[0])
            outTexCoords.add(tex[1])

            val newIdx = uniqueVertices.size.toShort()
            uniqueVertices[key] = newIdx
            return newIdx
        }

        try {
            context.assets.open(objFileName).bufferedReader().useLines { lines ->
                var currentColor = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("v ")) {
                        val parts = trimmed.substring(2).trim().split("\\s+".toRegex())
                        if (parts.size >= 3) {
                            val x = parts[0].toFloatOrNull() ?: 0f
                            val y = parts[1].toFloatOrNull() ?: 0f
                            val z = parts[2].toFloatOrNull() ?: 0f
                            rawPositions.add(Vector3(x, y, z))

                            // Check for embedded vertex color (v x y z r g b)
                            if (parts.size >= 6) {
                                var r = parts[3].toFloatOrNull() ?: 1.0f
                                var g = parts[4].toFloatOrNull() ?: 1.0f
                                var b = parts[5].toFloatOrNull() ?: 1.0f

                                // Coal Mine pathing: Recolor tracks and ties to slate dark coal on Stage 1 floor
                                if (objFileName.contains("frozen_mine") && y < -0.2f) {
                                    val isWood = r in 0.3f..0.5f && g in 0.15f..0.35f && b in 0.1f..0.22f
                                    val isRail = r in 0.5f..0.65f && g in 0.5f..0.65f && b in 0.5f..0.65f
                                    if (isWood || isRail) {
                                        r = 0.07f
                                        g = 0.07f
                                        b = 0.08f
                                    }
                                }
                                rawColors.add(floatArrayOf(r, g, b, 1.0f))
                            } else {
                                rawColors.add(floatArrayOf(-1.0f, -1.0f, -1.0f, -1.0f))
                            }
                        }
                    } else if (trimmed.startsWith("vt ")) {
                        val parts = trimmed.substring(3).trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            val u = parts[0].toFloatOrNull() ?: 0f
                            val v = parts[1].toFloatOrNull() ?: 0f
                            rawTexCoords.add(floatArrayOf(u, 1f - v))
                        }
                    } else if (trimmed.startsWith("vn ")) {
                        val parts = trimmed.substring(3).trim().split("\\s+".toRegex())
                        if (parts.size >= 3) {
                            val x = parts[0].toFloatOrNull() ?: 0f
                            val y = parts[1].toFloatOrNull() ?: 0f
                            val z = parts[2].toFloatOrNull() ?: 0f
                            rawNormals.add(Vector3(x, y, z))
                        }
                    } else if (trimmed.startsWith("usemtl ")) {
                        val matName = trimmed.substring("usemtl ".length).trim()
                        currentColor = materials[matName] ?: floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
                    } else if (trimmed.startsWith("f ")) {
                        val tokens = trimmed.split("\\s+".toRegex())
                        if (tokens.size >= 4) {
                            val idxList = tokens.drop(1).map { getOrCreateVertex(it, currentColor) }
                            for (i in 1 until idxList.size - 1) {
                                outIndices.add(idxList[0])
                                outIndices.add(idxList[i])
                                outIndices.add(idxList[i + 1])
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Models3D", "Error reading obj file: $objFileName", e)
        }

        val mesh = Mesh()
        mesh.initBuffers(
            outVertices.toFloatArray(),
            outNormals.toFloatArray(),
            outColors.toFloatArray(),
            outIndices.toShortArray()
        )
        if (outTexCoords.isNotEmpty()) {
            mesh.texCoordBuffer = ByteBuffer.allocateDirect(outTexCoords.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                    put(outTexCoords.toFloatArray())
                    position(0)
                }
        }
        mesh.textureName = textureFileName
        if (textureFileName != null) {
            mesh.silhouetteMode = 4
        }
        return mesh
    }
}
