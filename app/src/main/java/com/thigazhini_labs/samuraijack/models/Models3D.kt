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

        fun appendCone(
            x: Float, y: Float, z: Float,
            radius: Float, height: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f,
            segments: Int = 12
        ) {
            val startCenter = vertexCount.toShort()
            
            // Apex
            vertices.add(x)
            vertices.add(y + height / 2f)
            vertices.add(z)
            normals.add(0f, 1f, 0f)
            colors.add(r, g, b, a)
            vertexCount++

            // Base Center
            val baseCenter = vertexCount.toShort()
            vertices.add(x)
            vertices.add(y - height / 2f)
            vertices.add(z)
            normals.add(0f, -1f, 0f)
            colors.add(r, g, b, a)
            vertexCount++

            // Base Rim
            val rimStart = vertexCount
            for (i in 0 until segments) {
                val angle = (2 * Math.PI * i / segments).toFloat()
                val rx = (radius * Math.cos(angle.toDouble())).toFloat()
                val rz = (radius * Math.sin(angle.toDouble())).toFloat()

                vertices.add(x + rx)
                vertices.add(y - height / 2f)
                vertices.add(z + rz)

                val len = Math.sqrt((rx * rx + rz * rz).toDouble()).toFloat()
                normals.add(rx / len, 0.2f, rz / len)
                
                colors.add(r, g, b, a)
                vertexCount++
            }

            for (i in 0 until segments) {
                val nextIdx = rimStart + ((i + 1) % segments)
                indices.add(startCenter)
                indices.add((rimStart + i).toShort())
                indices.add(nextIdx.toShort())

                indices.add(baseCenter)
                indices.add(nextIdx.toShort())
                indices.add((rimStart + i).toShort())
            }
        }

        fun appendCylinder(
            x: Float, y: Float, z: Float,
            radius: Float, height: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f,
            segments: Int = 12
        ) {
            val halfH = height / 2f
            val bottomCenter = vertexCount.toShort()
            vertices.add(x)
            vertices.add(y - halfH)
            vertices.add(z)
            normals.add(0f, -1f, 0f)
            colors.add(r, g, b, a)
            vertexCount++

            val topCenter = vertexCount.toShort()
            vertices.add(x)
            vertices.add(y + halfH)
            vertices.add(z)
            normals.add(0f, 1f, 0f)
            colors.add(r, g, b, a)
            vertexCount++

            val bottomStart = vertexCount
            for (i in 0 until segments) {
                val angle = (2 * Math.PI * i / segments).toFloat()
                val rx = (radius * Math.cos(angle.toDouble())).toFloat()
                val rz = (radius * Math.sin(angle.toDouble())).toFloat()
                vertices.add(x + rx)
                vertices.add(y - halfH)
                vertices.add(z + rz)
                normals.add(0f, -1f, 0f)
                colors.add(r, g, b, a)
                vertexCount++
            }

            val topStart = vertexCount
            for (i in 0 until segments) {
                val angle = (2 * Math.PI * i / segments).toFloat()
                val rx = (radius * Math.cos(angle.toDouble())).toFloat()
                val rz = (radius * Math.sin(angle.toDouble())).toFloat()
                vertices.add(x + rx)
                vertices.add(y + halfH)
                vertices.add(z + rz)
                normals.add(0f, 1f, 0f)
                colors.add(r, g, b, a)
                vertexCount++
            }

            val sideStart = vertexCount
            for (i in 0 until segments) {
                val angle = (2 * Math.PI * i / segments).toFloat()
                val rx = (radius * Math.cos(angle.toDouble())).toFloat()
                val rz = (radius * Math.sin(angle.toDouble())).toFloat()
                val nx = rx / radius
                val nz = rz / radius

                vertices.add(x + rx)
                vertices.add(y - halfH)
                vertices.add(z + rz)
                normals.add(nx, 0f, nz)
                colors.add(r, g, b, a)
                vertexCount++

                vertices.add(x + rx)
                vertices.add(y + halfH)
                vertices.add(z + rz)
                normals.add(nx, 0f, nz)
                colors.add(r, g, b, a)
                vertexCount++
            }

            for (i in 0 until segments) {
                val next = (i + 1) % segments
                indices.add(bottomCenter)
                indices.add((bottomStart + next).toShort())
                indices.add((bottomStart + i).toShort())

                indices.add(topCenter)
                indices.add((topStart + i).toShort())
                indices.add((topStart + next).toShort())

                val sCurrentB = sideStart + i * 2
                val sCurrentT = sideStart + i * 2 + 1
                val sNextB = sideStart + next * 2
                val sNextT = sideStart + next * 2 + 1

                indices.add(sCurrentB.toShort())
                indices.add(sCurrentT.toShort())
                indices.add(sNextT.toShort())

                indices.add(sCurrentB.toShort())
                indices.add(sNextT.toShort())
                indices.add(sNextB.toShort())
            }
        }

        fun appendSphere(
            x: Float, y: Float, z: Float,
            radius: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f,
            rings: Int = 10, sectors: Int = 12
        ) {
            val startV = vertexCount
            val R = 1f / (rings - 1).toFloat()
            val S = 1f / (sectors - 1).toFloat()

            for (rIdx in 0 until rings) {
                val phi = (Math.PI * rIdx * R)
                val sinPhi = Math.sin(phi)
                val cosPhi = Math.cos(phi)

                for (sIdx in 0 until sectors) {
                    val theta = (2.0 * Math.PI * sIdx * S)
                    val sinTheta = Math.sin(theta)
                    val cosTheta = Math.cos(theta)

                    val vx = (radius * sinTheta * sinPhi).toFloat()
                    val vy = (radius * cosPhi).toFloat()
                    val vz = (radius * cosTheta * sinPhi).toFloat()

                    vertices.add(x + vx)
                    vertices.add(y + vy)
                    vertices.add(z + vz)

                    val len = Math.sqrt((vx * vx + vy * vy + vz * vz).toDouble()).toFloat()
                    normals.add(vx / len, vy / len, vz / len)

                    colors.add(r, g, b, a)
                    vertexCount++
                }
            }

            for (rIdx in 0 until rings - 1) {
                for (sIdx in 0 until sectors - 1) {
                    val k1 = startV + rIdx * sectors + sIdx
                    val k2 = k1 + sectors

                    indices.add(k1.toShort())
                    indices.add(k2.toShort())
                    indices.add((k1 + 1).toShort())

                    indices.add((k1 + 1).toShort())
                    indices.add(k2.toShort())
                    indices.add((k2 + 1).toShort())
                }
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

    fun createJack(isAttacking: Boolean = false): Mesh {
        val mb = MeshBuilder()
        // High-fidelity Jack with smooth curves and diagonal sword sheath on his back
        // Body (Smooth taper cylinder)
        mb.appendCylinder(0f, 0.45f, 0f, 0.22f, 0.9f, 1.0f, 1.0f, 1.0f, segments = 12)
        // Obi (Black Belt)
        mb.appendCylinder(0f, 0.38f, 0f, 0.23f, 0.12f, 0.1f, 0.1f, 0.12f, segments = 12)
        
        // Head (Smooth UV Sphere)
        mb.appendSphere(0f, 1.15f, 0f, 0.18f, 0.95f, 0.8f, 0.7f, 1.0f, 10, 12)
        // Hair (Sleek black hair bun + top knot)
        mb.appendSphere(0f, 1.3f, -0.06f, 0.12f, 0.05f, 0.05f, 0.06f, 1.0f, 8, 10)
        mb.appendCylinder(0f, 1.4f, -0.1f, 0.03f, 0.08f, 0.05f, 0.05f, 0.06f, segments = 8)
        
        // Limbs (Smooth Cylinders)
        mb.appendCylinder(-0.1f, -0.12f, 0f, 0.06f, 0.24f, 0.95f, 0.8f, 0.7f, segments = 8)
        mb.appendCylinder(0.1f, -0.12f, 0f, 0.06f, 0.24f, 0.95f, 0.8f, 0.7f, segments = 8)
        mb.appendBox(-0.1f, -0.25f, 0.04f, 0.12f, 0.03f, 0.18f, 0.5f, 0.35f, 0.2f) // sandals
        mb.appendBox(0.1f, -0.25f, 0.04f, 0.12f, 0.03f, 0.18f, 0.5f, 0.35f, 0.2f)

        if (isAttacking) {
            // Katana in hand (Unsheathed)
            // Handle
            mb.appendCylinder(0.25f, 0.6f, 0.15f, 0.02f, 0.18f, 0.2f, 0.2f, 0.2f, segments = 8)
            // Guard
            mb.appendSphere(0.25f, 0.7f, 0.15f, 0.04f, 0.8f, 0.6f, 0.1f, 1.0f, 8, 8)
            // Glowing cyan Katana Blade
            mb.appendBox(0.25f, 1.15f, 0.15f, 0.02f, 0.80f, 0.05f, 0.7f, 0.9f, 1.0f)
        } else {
            // Katana sheathed diagonally on his back (Prince of Persia style)
            // Sheath
            mb.appendBox(0.1f, 0.6f, -0.18f, 0.04f, 0.65f, 0.04f, 0.15f, 0.15f, 0.17f)
            // Guard
            mb.appendSphere(0.1f, 0.95f, -0.18f, 0.04f, 0.8f, 0.6f, 0.1f, 1.0f, 8, 8)
            // Handle sticking out
            mb.appendCylinder(0.1f, 1.05f, -0.18f, 0.02f, 0.18f, 0.2f, 0.2f, 0.2f, segments = 8)
        }

        return mb.build()
    }

    fun createAku(): Mesh {
        val mb = MeshBuilder()
        // High-fidelity smooth spiky Aku
        // Main Body (Smooth tapered tower)
        mb.appendCylinder(0f, 1.8f, 0f, 0.45f, 3.6f, 0.05f, 0.05f, 0.06f, segments = 12)
        // Green Face Mask
        mb.appendSphere(0f, 3.4f, 0.35f, 0.28f, 0.1f, 0.7f, 0.2f, 1.0f, 10, 10)
        // Eyes (Glowing red/yellow spheres)
        mb.appendSphere(-0.16f, 3.52f, 0.52f, 0.08f, 0.9f, 0.1f, 0.1f, 1.0f, 8, 8)
        mb.appendSphere(0.16f, 3.52f, 0.52f, 0.08f, 0.9f, 0.1f, 0.1f, 1.0f, 8, 8)
        // Spiky horns (smooth curved cones)
        mb.appendCone(-0.35f, 4.4f, 0f, 0.12f, 1.2f, 0.9f, 0.1f, 0.1f, segments = 8)
        mb.appendCone(0.35f, 4.4f, 0f, 0.12f, 1.2f, 0.9f, 0.1f, 0.1f, segments = 8)
        mb.appendCone(-0.55f, 4.7f, 0f, 0.08f, 0.8f, 0.95f, 0.3f, 0.0f, segments = 8)
        mb.appendCone(0.55f, 4.7f, 0f, 0.08f, 0.8f, 0.95f, 0.3f, 0.0f, segments = 8)

        return mb.build()
    }

    fun createRoboBeetle(): Mesh {
        val mb = MeshBuilder()
        // Sleek Sci-Fi capsule drone
        // Main Pod (Smooth UV Sphere)
        mb.appendSphere(0f, 0.3f, 0f, 0.25f, 0.28f, 0.3f, 0.34f, 1.0f, 10, 12)
        // Glowing cyan/red sensors
        mb.appendSphere(0f, 0.32f, 0.22f, 0.06f, 0.9f, 0.05f, 0.05f, 1.0f, 8, 8)
        // Mechanical legs (Smooth cylinders)
        mb.appendCylinder(-0.25f, 0.12f, 0.15f, 0.05f, 0.24f, 0.15f, 0.16f, 0.18f, segments = 8)
        mb.appendCylinder(0.25f, 0.12f, 0.15f, 0.05f, 0.24f, 0.15f, 0.16f, 0.18f, segments = 8)
        mb.appendCylinder(-0.25f, 0.12f, -0.15f, 0.05f, 0.24f, 0.15f, 0.16f, 0.18f, segments = 8)
        mb.appendCylinder(0.25f, 0.12f, -0.15f, 0.05f, 0.24f, 0.15f, 0.16f, 0.18f, segments = 8)

        return mb.build()
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
        // Majestic Prince of Persia style castle tower gateway
        // Left spired tower
        mb.appendCylinder(-2.2f, 2.0f, 0f, 0.6f, 4.0f, 0.3f, 0.3f, 0.32f, segments = 12)
        mb.appendCone(-2.2f, 4.5f, 0f, 0.8f, 1.0f, 0.6f, 0.15f, 0.15f, 1.0f, 12)
        
        // Right spired tower
        mb.appendCylinder(2.2f, 2.0f, 0f, 0.6f, 4.0f, 0.3f, 0.3f, 0.32f, segments = 12)
        mb.appendCone(2.2f, 4.5f, 0f, 0.8f, 1.0f, 0.6f, 0.15f, 0.15f, 1.0f, 12)

        // Middle Gate Archway (stone wall with dark passageway)
        mb.appendBox(0f, 1.5f, 0f, 3.8f, 3.0f, 0.8f, 0.25f, 0.25f, 0.27f)
        mb.appendBox(0f, 0.8f, 0.42f, 1.4f, 1.6f, 0.05f, 0.05f, 0.05f, 0.05f) // Dark door void
        
        // Battlement blocks on top
        mb.appendBox(-1.5f, 3.1f, 0f, 0.4f, 0.3f, 0.8f, 0.2f, 0.2f, 0.22f)
        mb.appendBox(-0.5f, 3.1f, 0f, 0.4f, 0.3f, 0.8f, 0.2f, 0.2f, 0.22f)
        mb.appendBox(0.5f, 3.1f, 0f, 0.4f, 0.3f, 0.8f, 0.2f, 0.2f, 0.22f)
        mb.appendBox(1.5f, 3.1f, 0f, 0.4f, 0.3f, 0.8f, 0.2f, 0.2f, 0.22f)

        return mb.build()
    }

    fun createLaser(r: Float = 0.95f, g: Float = 0.1f, b: Float = 0.1f): Mesh {
        val mb = MeshBuilder()
        mb.appendBox(0f, 0f, 0f, 0.08f, 0.08f, 0.4f, r, g, b)
        return mb.build()
    }

    fun createSwordSlashTrail(): Mesh {
        val mb = MeshBuilder()
        mb.appendBox(0f, 0f, 0f, 0.7f, 0.05f, 0.4f, 0.2f, 0.7f, 0.95f, 0.6f)
        return mb.build()
    }

    fun createSkyBackdrop(bgColor: FloatArray): Mesh {
        val mb = MeshBuilder()
        val r = bgColor[0]
        val g = bgColor[1]
        val b = bgColor[2]

        // Giant sky backdrop plane far away (Orange-red gradient sunset)
        val skyR = (r * 1.6f).coerceIn(0.4f, 1.0f)
        val skyG = (g * 1.4f).coerceIn(0.2f, 0.8f)
        val skyB = (b * 1.2f).coerceIn(0.1f, 0.6f)
        mb.appendBox(0f, 12f, 28f, 75f, 45f, 0.2f, skyR, skyG, skyB)

        // Large orange-yellow Sun/Moon
        mb.appendBox(6f, 9f, 27.5f, 11f, 11f, 0.1f, 0.98f, 0.62f, 0.15f)

        // Spired Pagoda/Castle Silhouettes in front of sun (Dark silhouettes)
        val silR = (r * 0.3f)
        val silG = (g * 0.3f)
        val silB = (b * 0.3f)
        // Spire 1
        mb.appendBox(-6f, 4f, 27f, 2.5f, 9f, 0.5f, silR, silG, silB)
        mb.appendCone(-6f, 9f, 27f, 1.6f, 2.0f, silR, silG, silB, segments = 8)
        // Spire 2
        mb.appendBox(-10f, 3f, 27f, 2.0f, 7f, 0.5f, silR, silG, silB)
        mb.appendCone(-10f, 6.8f, 27f, 1.2f, 1.6f, silR, silG, silB, segments = 8)
        // Spire 3 (Right side)
        mb.appendBox(12f, 2.5f, 27f, 3.0f, 5.0f, 0.5f, silR, silG, silB)
        mb.appendCone(12f, 5.2f, 27f, 1.8f, 1.8f, silR, silG, silB, segments = 8)

        return mb.build()
    }

    fun createTexturedSkyBackdrop(): Mesh {
        val mesh = Mesh()
        val halfW = 110f / 2f
        val halfH = 55f / 2f
        val yCenter = 10f
        val zCenter = 32f

        val vertices = floatArrayOf(
            -halfW, yCenter - halfH, zCenter,
             halfW, yCenter - halfH, zCenter,
             halfW, yCenter + halfH, zCenter,
            -halfW, yCenter + halfH, zCenter
        )

        val normals = floatArrayOf(
            0f, 0f, -1f,
            0f, 0f, -1f,
            0f, 0f, -1f,
            0f, 0f, -1f
        )

        val colors = floatArrayOf(
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f
        )

        val indices = shortArrayOf(
            0, 1, 2,
            0, 2, 3
        )

        val texCoords = floatArrayOf(
            0f, 1f,
            1f, 1f,
            1f, 0f,
            0f, 0f
        )

        mesh.initBuffers(vertices, normals, colors, indices)

        mesh.texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(texCoords)
                position(0)
            }
        mesh.silhouetteMode = 3 // Textured mode
        return mesh
    }
}
