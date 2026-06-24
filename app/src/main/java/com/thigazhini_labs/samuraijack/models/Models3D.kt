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

    fun createMineSupportFrame(
        width: Float = 5.8f,
        height: Float = 3.4f,
        depth: Float = 0.55f
    ): Mesh {
        val mb = MeshBuilder()
        val postX = width / 2f - 0.18f
        val postY = height / 2f
        val woodR = 0.31f
        val woodG = 0.21f
        val woodB = 0.14f

        mb.appendBox(-postX, postY, 0f, 0.32f, height, depth, woodR, woodG, woodB)
        mb.appendBox(postX, postY, 0f, 0.32f, height, depth, woodR, woodG, woodB)
        mb.appendBox(0f, height - 0.18f, 0f, width, 0.36f, depth, woodR, woodG, woodB)
        mb.appendBox(0f, height - 0.56f, 0f, width * 0.82f, 0.2f, depth * 0.82f, 0.24f, 0.17f, 0.11f)

        // Diagonal reinforcement beams
        mb.appendBox(-width * 0.26f, height * 0.6f, 0f, 0.2f, 1.2f, depth * 0.76f, 0.25f, 0.17f, 0.12f)
        mb.appendBox(width * 0.26f, height * 0.6f, 0f, 0.2f, 1.2f, depth * 0.76f, 0.25f, 0.17f, 0.12f)

        // Broken and patched planks to avoid perfect symmetry
        mb.appendBox(-width * 0.12f, height * 0.45f, 0.02f, width * 0.2f, 0.12f, depth * 0.66f, 0.22f, 0.15f, 0.1f)
        mb.appendBox(width * 0.14f, height * 0.4f, -0.03f, width * 0.22f, 0.1f, depth * 0.62f, 0.19f, 0.13f, 0.09f)

        // Metal bolts and brackets
        val boltR = 0.2f
        val boltG = 0.2f
        val boltB = 0.22f
        for (x in floatArrayOf(-postX, postX)) {
            mb.appendSphere(x, height - 0.3f, depth * 0.26f, 0.05f, boltR, boltG, boltB, 1f, 6, 7)
            mb.appendSphere(x, 0.9f, depth * 0.26f, 0.05f, boltR, boltG, boltB, 1f, 6, 7)
        }

        // Rope wrapping for ad-hoc reinforcement
        mb.appendCylinder(-postX, 1.35f, 0f, 0.04f, 0.28f, 0.34f, 0.26f, 0.15f, segments = 7)
        mb.appendCylinder(postX, 1.15f, 0f, 0.04f, 0.24f, 0.34f, 0.26f, 0.15f, segments = 7)

        // Wood grain strips, splits, and damaged edges to avoid cube look
        for (x in floatArrayOf(-postX, postX)) {
            for (i in 0 until 5) {
                val y = 0.45f + i * 0.62f
                val offset = if (x < 0f) -0.01f else 0.01f
                mb.appendBox(x + offset, y, depth * 0.27f, 0.02f, 0.46f, 0.03f, 0.2f, 0.14f, 0.1f)
            }
            mb.appendBox(x, 1.22f, depth * 0.29f, 0.04f, 0.54f, 0.02f, 0.08f, 0.06f, 0.05f)
            mb.appendBox(x, 2.08f, depth * 0.28f, 0.03f, 0.4f, 0.02f, 0.08f, 0.06f, 0.05f)
            mb.appendSphere(x - if (x < 0f) -0.06f else 0.06f, 0.24f, 0.18f, 0.06f, 0.18f, 0.12f, 0.08f, 1f, 6, 7)
        }

        // Nails and rough metal straps
        for (x in floatArrayOf(-postX, postX)) {
            mb.appendSphere(x, 2.95f, 0.2f, 0.03f, 0.26f, 0.26f, 0.28f, 1f, 6, 7)
            mb.appendSphere(x, 1.7f, 0.2f, 0.03f, 0.26f, 0.26f, 0.28f, 1f, 6, 7)
        }
        mb.appendBox(-width * 0.3f, height - 0.24f, 0.22f, 0.55f, 0.06f, 0.03f, 0.2f, 0.2f, 0.22f)
        mb.appendBox(width * 0.3f, height - 0.24f, 0.22f, 0.55f, 0.06f, 0.03f, 0.2f, 0.2f, 0.22f)
        return mb.build()
    }

    fun createMineLanternGlow(r: Float = 0.98f, g: Float = 0.74f, b: Float = 0.28f): Mesh {
        val mb = MeshBuilder()
        mb.appendCylinder(0f, 1.22f, 0f, 0.03f, 0.26f, 0.2f, 0.15f, 0.1f, segments = 7)
        mb.appendBox(0f, 0.94f, 0f, 0.23f, 0.26f, 0.23f, 0.13f, 0.11f, 0.1f)
        mb.appendBox(0f, 0.84f, 0f, 0.16f, 0.42f, 0.16f, 0.2f, 0.16f, 0.12f)
        mb.appendSphere(0f, 0.8f, 0f, 0.14f, r, g, b, 1.0f, 9, 9)
        mb.appendSphere(0f, 0.8f, 0f, 0.08f, 1f, 0.86f, 0.46f, 1.0f, 8, 8)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    fun createMineCart(): Mesh {
        val mb = MeshBuilder()
        mb.appendBox(0f, 0.46f, 0f, 1.35f, 0.44f, 0.92f, 0.16f, 0.13f, 0.11f)
        mb.appendBox(0f, 0.68f, 0f, 1.15f, 0.18f, 0.72f, 0.22f, 0.18f, 0.15f)
        mb.appendBox(0f, 0.32f, 0f, 1.45f, 0.08f, 1.02f, 0.1f, 0.08f, 0.07f)
        mb.appendBox(0f, 0.56f, 0.44f, 1.05f, 0.2f, 0.08f, 0.3f, 0.24f, 0.17f)

        val wheelColor = floatArrayOf(0.18f, 0.1f, 0.08f)
        mb.appendCylinder(-0.58f, 0.18f, 0.38f, 0.14f, 0.08f, wheelColor[0], wheelColor[1], wheelColor[2], segments = 10)
        mb.appendCylinder(0.58f, 0.18f, 0.38f, 0.14f, 0.08f, wheelColor[0], wheelColor[1], wheelColor[2], segments = 10)
        mb.appendCylinder(-0.58f, 0.18f, -0.38f, 0.14f, 0.08f, wheelColor[0], wheelColor[1], wheelColor[2], segments = 10)
        mb.appendCylinder(0.58f, 0.18f, -0.38f, 0.14f, 0.08f, wheelColor[0], wheelColor[1], wheelColor[2], segments = 10)
        mb.appendCylinder(0f, 0.2f, 0.38f, 0.03f, 1.16f, 0.2f, 0.12f, 0.1f, segments = 7)
        mb.appendCylinder(0f, 0.2f, -0.38f, 0.03f, 1.16f, 0.2f, 0.12f, 0.1f, segments = 7)
        return mb.build()
    }

    fun createMiningRailSection(length: Float = 8f, sleeperCount: Int = 7): Mesh {
        val mb = MeshBuilder()
        val halfLen = length / 2f
        val railSpacing = 1.32f

        // Proper I-beam rail profile (base flange → web → polished head)
        val rFlange  = floatArrayOf(0.28f, 0.20f, 0.14f)   // rusty dirty flange
        val rWeb     = floatArrayOf(0.36f, 0.26f, 0.19f)   // rust web
        val rHead    = floatArrayOf(0.52f, 0.46f, 0.40f)   // polished-steel top

        for (sign in floatArrayOf(-1f, 1f)) {
            val rx = sign * railSpacing / 2f
            mb.appendBox(rx, 0.03f, 0f,  0.22f, 0.04f, length, rFlange[0], rFlange[1], rFlange[2])
            mb.appendBox(rx, 0.09f, 0f,  0.05f, 0.10f, length, rWeb[0],    rWeb[1],    rWeb[2])
            mb.appendBox(rx, 0.17f, 0f,  0.17f, 0.06f, length, rHead[0],   rHead[1],   rHead[2])
        }

        // Compacted coal-dust trackbed
        mb.appendBox(0f, -0.03f, 0f, railSpacing + 1.18f, 0.08f, length, 0.08f, 0.08f, 0.09f)

        // Sleepers — varied wood tones from dark to weathered
        for (i in 0 until sleeperCount) {
            val t = if (sleeperCount == 1) 0f else i.toFloat() / (sleeperCount - 1).toFloat()
            val z = -halfLen + t * length
            val sleeperY = if (i % 2 == 0) 0.0f else -0.008f
            // Vary color: some dark, some weathered tan, some nearly black
            val woodVar = i % 4
            val (wr, wg, wb) = when (woodVar) {
                0 -> Triple(0.15f, 0.09f, 0.06f)    // dark brown
                1 -> Triple(0.22f, 0.14f, 0.08f)    // medium weathered
                2 -> Triple(0.10f, 0.06f, 0.04f)    // nearly black
                else -> Triple(0.18f, 0.11f, 0.07f)  // mid-tone
            }
            mb.appendBox(0f, sleeperY, z, railSpacing + 0.68f, 0.10f, 0.26f, wr, wg, wb)
        }

        // Compacted coal ballast bed, darker and less uniform
        mb.appendBox(0f, -0.03f, 0f, railSpacing + 1.86f, 0.11f, length, 0.1f, 0.1f, 0.11f)
        mb.appendBox(-(railSpacing * 0.86f), 0.01f, 0f, 0.7f, 0.12f, length, 0.09f, 0.09f, 0.1f)
        mb.appendBox((railSpacing * 0.86f), 0.01f, 0f, 0.7f, 0.12f, length, 0.09f, 0.09f, 0.1f)

        // Realistic ballast mix: 70% small, 20% medium, 10% large
        val chunkCount = (length * 11f).toInt().coerceAtLeast(88)
        val gravelColors = arrayOf(
            floatArrayOf(0.09f, 0.09f, 0.1f),
            floatArrayOf(0.12f, 0.12f, 0.13f),
            floatArrayOf(0.08f, 0.08f, 0.09f),
            floatArrayOf(0.14f, 0.14f, 0.16f),
            floatArrayOf(0.1f, 0.1f, 0.11f),
            floatArrayOf(0.11f, 0.1f, 0.09f)
        )
        for (i in 0 until chunkCount) {
            val ratio = i.toFloat() / chunkCount.toFloat()
            val z = -halfLen + ratio * length
            val phase = i.toFloat() * 0.42f
            val sizeBand = i % 10
            val baseRadius = when {
                sizeBand < 7 -> 0.022f + (i % 3) * 0.006f   // 70% small
                sizeBand < 9 -> 0.036f + (i % 2) * 0.008f   // 20% medium
                else -> 0.054f + (i % 2) * 0.01f            // 10% large
            }
            val col = gravelColors[i % gravelColors.size]

            mb.appendSphere(kotlin.math.sin(phase) * (railSpacing * 0.35f), 0.037f + (i % 3) * 0.004f, z,
                baseRadius, col[0], col[1], col[2], 1f, 6, 7)
            val col2 = gravelColors[(i + 3) % gravelColors.size]
            mb.appendSphere(-(railSpacing * 0.86f) + kotlin.math.sin(phase * 1.5f) * 0.24f,
                0.042f + (i % 2) * 0.004f, z + 0.05f, baseRadius + 0.008f, col2[0], col2[1], col2[2], 1f, 6, 7)
            val col3 = gravelColors[(i + 5) % gravelColors.size]
            mb.appendSphere((railSpacing * 0.86f) + kotlin.math.cos(phase * 1.4f) * 0.25f,
                0.042f + ((i + 1) % 2) * 0.004f, z - 0.05f, baseRadius + 0.008f, col3[0], col3[1], col3[2], 1f, 6, 7)
        }
        return mb.build()
    }

    fun createTunnelRockModule(width: Float = 8.8f, height: Float = 5.2f, depth: Float = 8f): Mesh {
        val mb = MeshBuilder()
        val halfW = width / 2f

        val base      = floatArrayOf(0.07f,  0.08f,  0.09f)
        val rock1     = floatArrayOf(0.12f,  0.13f,  0.15f)
        val rock2     = floatArrayOf(0.16f,  0.17f,  0.19f)
        val rock3     = floatArrayOf(0.10f,  0.11f,  0.13f)
        val coal      = floatArrayOf(0.03f,  0.03f,  0.04f)
        val coalGloss = floatArrayOf(0.06f,  0.06f,  0.07f)

        // Thin background wall panels (behind all the rock)
        mb.appendBox(-halfW + 0.3f, 1.8f, 0f, 0.6f, height + 0.4f, depth + 0.2f, base[0], base[1], base[2])
        mb.appendBox( halfW - 0.3f, 1.8f, 0f, 0.6f, height + 0.4f, depth + 0.2f, base[0], base[1], base[2])
        mb.appendBox(0f, height - 0.28f, 0f, width - 1.1f, 1.1f, depth + 0.2f, 0.06f, 0.07f, 0.08f)

        // ── LEFT WALL — Massive rocky bed surface with dense pebble coverage ───────
        val lw = -halfW + 0.6f

        // 15 massive slabs forming the rocky base
        for (i in 0 until 15) {
            val z    = -depth * 0.50f + i * (depth * 0.070f)
            val sH   = 0.8f  + (i % 5) * 0.56f
            val sW   = 0.72f + (i % 6) * 0.24f
            val sD   = 0.58f + (i % 4) * 0.36f
            val sY   = 0.35f + (i % 7) * 0.58f
            val off  = (i % 4) * 0.10f
            val c    = when (i % 4) { 0 -> rock2; 1 -> rock1; 2 -> rock3; else -> rock2 }
            mb.appendBox(lw + sW * 0.5f + off, sY, z, sW, sH, sD, c[0], c[1], c[2])
        }
        
        // 6 mega override slabs for dramatic depth
        mb.appendBox(lw + 0.72f, 2.8f, -depth * 0.36f, 1.3f, 2.2f, 1.1f, rock2[0], rock2[1], rock2[2])
        mb.appendBox(lw + 0.84f, 0.9f,  depth * 0.30f,  1.0f, 1.6f, 0.95f, rock1[0], rock1[1], rock1[2])
        mb.appendBox(lw + 0.64f, 2.2f, -depth * 0.08f, 1.4f, 3.0f, 1.6f, rock3[0], rock3[1], rock3[2])
        mb.appendBox(lw + 0.58f, 3.85f, depth * 0.18f,  0.95f, 1.2f, 0.80f, rock2[0], rock2[1], rock2[2])
        mb.appendBox(lw + 1.0f,  1.5f,  -depth * 0.42f, 1.1f, 2.0f, 0.9f, rock1[0], rock1[1], rock1[2])
        mb.appendBox(lw + 0.68f, 3.4f,  depth * 0.05f,  1.0f, 1.8f, 1.2f, rock3[0], rock3[1], rock3[2])

        // Boulder protrusions
        for (i in 0 until 8) {
            val z = -depth * 0.48f + i * (depth * 0.14f)
            val y = 0.8f + (i % 5) * 0.9f
            val r = 0.40f + (i % 4) * 0.18f
            val c = when (i % 3) { 0 -> rock2; 1 -> rock1; else -> rock3 }
            mb.appendSphere(lw + 0.72f + (i % 2) * 0.1f, y, z, r, c[0], c[1], c[2], 1f, 9, 10)
        }

        // 40+ tiny pebble clusters packed densely
        for (i in 0 until 40) {
            val z2 = -depth * 0.49f + i * (depth * 0.042f)
            val y2 = 0.2f + (i % 10) * 0.52f
            val r2 = 0.06f + (i % 7) * 0.05f
            val c2 = when (i % 5) { 0 -> rock2; 1 -> rock1; 2 -> rock3; 3 -> coal; else -> rock1 }
            mb.appendSphere(lw + r2 + 0.06f + (i % 3) * 0.04f, y2, z2, r2, c2[0], c2[1], c2[2], 1f, 7, 8)
        }

        // Coal seams
        mb.appendBox(lw + 0.32f, 2.6f, -depth * 0.30f, 0.12f, 3.2f, 0.48f, coal[0], coal[1], coal[2])
        mb.appendBox(lw + 0.42f, 1.4f,  depth * 0.38f, 0.12f, 2.2f, 0.42f, coal[0], coal[1], coal[2])
        mb.appendBox(lw + 0.52f, 3.4f, -depth * 0.08f, 0.14f, 1.8f, 1.2f, coal[0], coal[1], coal[2])
        mb.appendSphere(lw + 0.58f, 2.1f,  depth * 0.14f, 0.26f, coal[0],      coal[1],      coal[2],      1f, 8, 9)
        mb.appendSphere(lw + 0.50f, 3.3f, -depth * 0.26f, 0.24f, coalGloss[0], coalGloss[1], coalGloss[2], 1f, 8, 9)

        // Cracks
        mb.appendBox(lw + 0.10f, 1.6f, -depth * 0.14f, 0.02f, 0.8f, 1.0f, 0.02f, 0.02f, 0.03f)
        mb.appendBox(lw + 0.09f, 3.0f,  depth * 0.20f, 0.02f, 0.6f, 0.9f, 0.02f, 0.02f, 0.03f)
        // ── RIGHT WALL — Dense rocky bed with massive detail ─────────────────────
        val rw = halfW - 0.6f

        // 15 massive slabs
        for (i in 0 until 15) {
            val z    = depth * 0.50f - i * (depth * 0.070f)
            val sH   = 0.75f + (i % 5) * 0.60f
            val sW   = 0.70f + (i % 6) * 0.26f
            val sD   = 0.55f + (i % 4) * 0.38f
            val sY   = 0.40f + (i % 7) * 0.54f
            val off  = (i % 4) * 0.09f
            val c    = when (i % 4) { 0 -> rock1; 1 -> rock3; 2 -> rock2; else -> rock1 }
            mb.appendBox(rw - sW * 0.5f - off, sY, z, sW, sH, sD, c[0], c[1], c[2])
        }
        // 6 mega slabs
        mb.appendBox(rw - 0.75f, 1.95f, depth * 0.32f,  1.2f, 1.8f, 0.96f, rock1[0], rock1[1], rock1[2])
        mb.appendBox(rw - 0.68f, 3.4f, -depth * 0.20f, 1.3f, 1.4f, 0.82f, rock2[0], rock2[1], rock2[2])
        mb.appendBox(rw - 0.62f, 0.8f,  depth * 0.08f, 1.5f, 2.4f, 1.7f,  rock3[0], rock3[1], rock3[2])
        mb.appendBox(rw - 0.66f, 2.9f, -depth * 0.42f, 1.0f, 1.05f, 0.65f, rock1[0], rock1[1], rock1[2])
        mb.appendBox(rw - 0.95f, 4.0f,  depth * 0.20f,  0.92f, 1.1f, 0.75f, rock2[0], rock2[1], rock2[2])
        mb.appendBox(rw - 0.72f, 1.3f, -depth * 0.06f, 1.1f, 1.9f, 1.0f, rock3[0], rock3[1], rock3[2])

        // Boulder protrusions right
        for (i in 0 until 8) {
            val z = depth * 0.48f - i * (depth * 0.14f)
            val y = 0.75f + (i % 5) * 0.95f
            val r = 0.42f + (i % 4) * 0.16f
            val c = when (i % 3) { 0 -> rock1; 1 -> rock2; else -> rock3 }
            mb.appendSphere(rw - 0.74f - (i % 2) * 0.1f, y, z, r, c[0], c[1], c[2], 1f, 9, 10)
        }

        // 40+ pebble clusters right
        for (i in 0 until 40) {
            val z2 = depth * 0.49f - i * (depth * 0.042f)
            val y2 = 0.25f + (i % 10) * 0.50f
            val r2 = 0.06f + (i % 7) * 0.05f
            val c2 = when (i % 5) { 0 -> rock1; 1 -> rock2; 2 -> rock3; 3 -> coal; else -> rock3 }
            mb.appendSphere(rw - r2 - 0.06f - (i % 3) * 0.04f, y2, z2, r2, c2[0], c2[1], c2[2], 1f, 7, 8)
        }

        mb.appendBox(rw - 0.34f, 1.9f,  depth * 0.24f, 0.12f, 2.6f, 0.44f, coal[0], coal[1], coal[2])
        mb.appendBox(rw - 0.44f, 3.1f, -depth * 0.38f, 0.12f, 1.9f, 1.1f,  coal[0], coal[1], coal[2])
        mb.appendSphere(rw - 0.48f, 2.7f,  depth * 0.18f, 0.22f, coal[0],      coal[1],      coal[2],      1f, 8, 9)
        mb.appendSphere(rw - 0.54f, 1.5f, -depth * 0.34f, 0.20f, coalGloss[0], coalGloss[1], coalGloss[2], 1f, 8, 9)
        mb.appendBox(rw - 0.10f, 2.2f,  depth * 0.12f, 0.02f, 0.9f, 1.0f, 0.02f, 0.02f, 0.03f)

        // ── CEILING ─────────────────────────────────────────────────────────────
        mb.appendBox(0f, height - 0.28f, 0f, width - 1.1f, 0.6f, depth + 0.2f, base[0], base[1], base[2])
        for (i in 0 until 7) {
            val z  = -depth * 0.44f + i * (depth * 0.15f)
            val cW = 1.6f + (i % 3) * 0.7f
            val cD = 0.6f + (i % 2) * 0.35f
            val cY = height - 0.5f - (i % 3) * 0.16f
            mb.appendBox(if (i % 2 == 0) -0.6f else 0.5f, cY, z, cW, 0.32f + (i % 3) * 0.12f, cD, rock1[0], rock1[1], rock1[2])
        }
        for (i in 0 until 6) {
            val z    = -depth * 0.38f + i * (depth * 0.16f)
            val x    = if (i % 2 == 0) -1.1f + (i % 3) * 0.5f else 0.8f - (i % 3) * 0.4f
            val sLen = 0.22f + (i % 3) * 0.18f
            mb.appendCone(x, height - 0.72f - sLen / 2f, z, 0.08f + (i % 2) * 0.04f, sLen,
                rock3[0], rock3[1], rock3[2], 1f, 7)
        }

        // Coal veins, wall cracks, and jagged protrusions to break boxy silhouettes
        for (i in 0 until 6) {
            val z = -depth * 0.42f + i * (depth * 0.16f)
            val y = 0.9f + (i % 4) * 0.72f
            mb.appendBox(-halfW + 0.58f, y, z, 0.04f, 0.34f + (i % 2) * 0.14f, 0.82f, 0.03f, 0.03f, 0.04f)
            mb.appendBox(halfW - 0.58f, y + 0.18f, z - 0.2f, 0.04f, 0.28f + (i % 3) * 0.1f, 0.72f, 0.03f, 0.03f, 0.04f)
        }
        for (i in 0 until 8) {
            val z = -depth * 0.45f + i * (depth * 0.13f)
            val y = 0.45f + (i % 5) * 0.58f
            val leftInset = 0.32f + (i % 3) * 0.12f
            val rightInset = 0.34f + ((i + 1) % 3) * 0.11f
            mb.appendCone(-halfW + leftInset, y, z, 0.12f + (i % 2) * 0.04f, 0.38f, rock2[0], rock2[1], rock2[2], 1f, 7)
            mb.appendCone(halfW - rightInset, y + 0.1f, z - 0.14f, 0.1f + (i % 2) * 0.05f, 0.34f, rock1[0], rock1[1], rock1[2], 1f, 7)
        }
        mb.appendBox(-halfW + 0.52f, 1.8f, depth * 0.18f, 0.02f, 1.2f, 1.3f, 0.02f, 0.02f, 0.03f)
        mb.appendBox(halfW - 0.5f, 2.2f, -depth * 0.12f, 0.02f, 1.0f, 1.1f, 0.02f, 0.02f, 0.03f)

        return mb.build()
    }

    fun createCoalPile(radius: Float = 0.6f, heightScale: Float = 1f): Mesh {
        val mb = MeshBuilder()
        val coal = floatArrayOf(0.04f, 0.04f, 0.05f)
        mb.appendSphere(0f, 0.18f * heightScale, 0f, radius * 0.62f, coal[0], coal[1], coal[2], 1f, 8, 10)
        mb.appendSphere(-radius * 0.3f, 0.14f * heightScale, radius * 0.2f, radius * 0.45f, coal[0], coal[1], coal[2], 1f, 7, 8)
        mb.appendSphere(radius * 0.32f, 0.1f * heightScale, -radius * 0.28f, radius * 0.42f, 0.07f, 0.07f, 0.08f, 1f, 7, 8)
        mb.appendSphere(radius * 0.12f, 0.25f * heightScale, radius * 0.08f, radius * 0.2f, 0.1f, 0.1f, 0.12f, 1f, 7, 8)
        return mb.build()
    }

    fun createCoalDebrisPatch(size: Float = 1.25f): Mesh {
        val mb = MeshBuilder()
        val rock = floatArrayOf(0.08f, 0.08f, 0.09f)
        mb.appendBox(0f, -0.02f, 0f, size, 0.05f, size * 0.75f, 0.06f, 0.06f, 0.07f)
        mb.appendSphere(-size * 0.2f, 0.05f, size * 0.1f, 0.12f, rock[0], rock[1], rock[2], 1f, 6, 7)
        mb.appendSphere(size * 0.22f, 0.04f, -size * 0.15f, 0.09f, rock[0], rock[1], rock[2], 1f, 6, 7)
        mb.appendSphere(0f, 0.03f, -size * 0.22f, 0.08f, 0.12f, 0.12f, 0.14f, 1f, 6, 7)
        mb.appendSphere(size * 0.04f, 0.06f, size * 0.28f, 0.07f, 0.05f, 0.05f, 0.06f, 1f, 6, 7)
        mb.appendSphere(-size * 0.3f, 0.04f, -size * 0.28f, 0.06f, 0.05f, 0.05f, 0.06f, 1f, 6, 7)
        return mb.build()
    }

    fun createMineCartDetailed(overturned: Boolean = false, filled: Boolean = true, damaged: Boolean = false): Mesh {
        val cart = createMineCart()
        if (damaged) {
            cart.rotation.x = -4f
            cart.rotation.z = if (overturned) 24f else 9f
        }
        if (filled) {
            val mb = MeshBuilder()
            mb.appendSphere(0f, 0.78f, 0f, 0.42f, 0.06f, 0.06f, 0.07f, 1f, 8, 10)
            mb.appendSphere(-0.2f, 0.76f, 0.18f, 0.22f, 0.07f, 0.07f, 0.08f, 1f, 7, 8)
            mb.appendSphere(0.18f, 0.76f, -0.14f, 0.2f, 0.07f, 0.07f, 0.08f, 1f, 7, 8)
            if (damaged || overturned) {
                mb.appendSphere(0.64f, 0.08f, -0.28f, 0.16f, 0.06f, 0.06f, 0.07f, 1f, 7, 8)
                mb.appendSphere(0.78f, 0.06f, -0.1f, 0.11f, 0.06f, 0.06f, 0.07f, 1f, 7, 8)
            }
            val coalMesh = mb.build()
            coalMesh.position = Vector3(cart.position.x, cart.position.y, cart.position.z)
            if (overturned) {
                coalMesh.rotation.z = 24f
            }
            return mergeMeshes(cart, coalMesh)
        }
        if (overturned) {
            cart.rotation.z = 22f
        }
        return cart
    }

    fun createHangingRope(length: Float = 1.4f): Mesh {
        val mb = MeshBuilder()
        mb.appendCylinder(0f, length / 2f + 0.2f, 0f, 0.03f, length, 0.32f, 0.24f, 0.15f, segments = 7)
        mb.appendSphere(0f, 0.16f, 0f, 0.07f, 0.2f, 0.2f, 0.22f, 1f, 6, 7)
        return mb.build()
    }

    fun createBrokenBridgeSection(): Mesh {
        val mb = MeshBuilder()
        val woodR = 0.31f
        val woodG = 0.22f
        val woodB = 0.15f
        mb.appendBox(0f, 0.42f, 0f, 2.0f, 0.12f, 1.05f, woodR, woodG, woodB)
        mb.appendBox(-0.8f, 0.55f, 0f, 0.12f, 0.35f, 1.05f, 0.22f, 0.16f, 0.11f)
        mb.appendBox(0.65f, 0.5f, -0.2f, 0.12f, 0.26f, 0.75f, 0.22f, 0.16f, 0.11f)
        return mb.build()
    }

    fun createAbandonedToolSet(): Mesh {
        val mb = MeshBuilder()
        mb.appendCylinder(0f, 0.45f, 0f, 0.03f, 0.85f, 0.36f, 0.27f, 0.18f, segments = 7)
        mb.appendBox(0.12f, 0.78f, 0f, 0.26f, 0.08f, 0.1f, 0.42f, 0.42f, 0.46f)
        mb.appendCylinder(-0.18f, 0.38f, 0f, 0.025f, 0.7f, 0.34f, 0.26f, 0.17f, segments = 7)
        mb.appendBox(-0.28f, 0.68f, 0f, 0.18f, 0.18f, 0.08f, 0.4f, 0.4f, 0.43f)
        return mb.build()
    }

    fun createTreasureChest(): Mesh {
        val mb = MeshBuilder()
        mb.appendBox(0f, 0.28f, 0f, 0.95f, 0.48f, 0.62f, 0.24f, 0.17f, 0.1f)
        mb.appendCylinder(0f, 0.54f, 0f, 0.31f, 0.24f, 0.35f, 0.24f, 0.13f, segments = 10)
        mb.appendBox(0f, 0.38f, 0.32f, 0.2f, 0.12f, 0.06f, 0.74f, 0.58f, 0.2f)
        mb.appendSphere(0f, 0.36f, 0.32f, 0.04f, 0.92f, 0.75f, 0.22f, 1f, 7, 8)
        return mb.build()
    }

    fun createAncientMineArtifact(): Mesh {
        val mb = MeshBuilder()
        mb.appendBox(0f, 0.38f, 0f, 0.6f, 0.75f, 0.38f, 0.22f, 0.22f, 0.25f)
        mb.appendCylinder(0f, 0.9f, 0f, 0.16f, 0.25f, 0.62f, 0.08f, 0.08f, segments = 8)
        mb.appendBox(0f, 1.08f, 0f, 0.48f, 0.08f, 0.14f, 0.52f, 0.08f, 0.08f)
        return mb.build()
    }

    fun createDustMoteCluster(): Mesh {
        val mb = MeshBuilder()
        // Cold airborne frost-dust
        mb.appendSphere(-0.12f, 1.25f, 0f, 0.06f, 0.22f, 0.26f, 0.32f, 1f, 6, 7)
        mb.appendSphere(0.08f, 1.42f, 0.1f, 0.045f, 0.24f, 0.29f, 0.36f, 1f, 6, 7)
        mb.appendSphere(0f, 1.32f, -0.1f, 0.04f, 0.2f, 0.24f, 0.3f, 1f, 6, 7)
        mb.appendSphere(-0.18f, 1.1f, -0.08f, 0.035f, 0.21f, 0.25f, 0.32f, 1f, 6, 7)
        mb.appendSphere(0.16f, 1.2f, -0.06f, 0.04f, 0.23f, 0.28f, 0.35f, 1f, 6, 7)
        mb.appendSphere(0.03f, 1.55f, 0.04f, 0.03f, 0.28f, 0.34f, 0.42f, 1f, 6, 7)
        mb.appendSphere(-0.08f, 1.48f, 0.16f, 0.028f, 0.27f, 0.33f, 0.4f, 1f, 6, 7)
        mb.appendSphere(0.19f, 1.34f, 0.02f, 0.026f, 0.24f, 0.3f, 0.38f, 1f, 6, 7)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    fun createMineFogBank(): Mesh {
        val mb = MeshBuilder()
        // Low, cold mine haze
        mb.appendSphere(0f, 0.78f, 0f, 0.66f, 0.16f, 0.2f, 0.28f, 1f, 8, 8)
        mb.appendSphere(-0.42f, 0.68f, 0.16f, 0.5f, 0.15f, 0.19f, 0.27f, 1f, 8, 8)
        mb.appendSphere(0.4f, 0.72f, -0.14f, 0.46f, 0.15f, 0.19f, 0.27f, 1f, 8, 8)
        mb.appendSphere(0.14f, 0.98f, 0.08f, 0.36f, 0.18f, 0.23f, 0.32f, 1f, 8, 8)
        mb.appendSphere(-0.08f, 0.92f, -0.22f, 0.3f, 0.17f, 0.22f, 0.31f, 1f, 8, 8)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    fun createMiningSupplyCluster(): Mesh {
        val mb = MeshBuilder()
        // Crate
        mb.appendBox(-0.38f, 0.28f, -0.1f, 0.52f, 0.56f, 0.52f, 0.24f, 0.17f, 0.11f)
        mb.appendBox(-0.38f, 0.28f, -0.1f, 0.46f, 0.46f, 0.46f, 0.18f, 0.12f, 0.08f)
        // Barrel
        mb.appendCylinder(0.32f, 0.34f, 0.06f, 0.2f, 0.68f, 0.3f, 0.22f, 0.14f, segments = 10)
        mb.appendCylinder(0.32f, 0.18f, 0.06f, 0.22f, 0.04f, 0.16f, 0.14f, 0.14f, segments = 10)
        mb.appendCylinder(0.32f, 0.5f, 0.06f, 0.22f, 0.04f, 0.16f, 0.14f, 0.14f, segments = 10)
        // Pickaxe and shovel
        mb.appendCylinder(0f, 0.54f, 0.36f, 0.025f, 0.88f, 0.33f, 0.24f, 0.14f, segments = 7)
        mb.appendBox(0.12f, 0.93f, 0.36f, 0.24f, 0.06f, 0.08f, 0.36f, 0.36f, 0.39f)
        mb.appendCylinder(-0.14f, 0.5f, 0.31f, 0.022f, 0.78f, 0.33f, 0.24f, 0.14f, segments = 7)
        mb.appendBox(-0.24f, 0.84f, 0.31f, 0.16f, 0.16f, 0.06f, 0.34f, 0.34f, 0.37f)
        return mb.build()
    }

    fun createMineRubbleMound(): Mesh {
        val mb = MeshBuilder()
        mb.appendSphere(0f, 0.12f, 0f, 0.5f, 0.12f, 0.1f, 0.09f, 1f, 8, 9)
        mb.appendSphere(0.26f, 0.09f, -0.22f, 0.28f, 0.1f, 0.09f, 0.08f, 1f, 7, 8)
        mb.appendSphere(-0.28f, 0.08f, 0.18f, 0.24f, 0.09f, 0.08f, 0.08f, 1f, 7, 8)
        mb.appendSphere(-0.06f, 0.18f, 0.1f, 0.18f, 0.05f, 0.05f, 0.06f, 1f, 7, 8)
        mb.appendSphere(0.12f, 0.14f, -0.1f, 0.14f, 0.05f, 0.05f, 0.06f, 1f, 7, 8)
        return mb.build()
    }

    fun createBrokenPlankPile(): Mesh {
        val mb = MeshBuilder()
        mb.appendBox(0f, 0.08f, 0f, 1.1f, 0.08f, 0.26f, 0.23f, 0.16f, 0.1f)
        mb.appendBox(0.18f, 0.14f, -0.1f, 0.86f, 0.07f, 0.2f, 0.21f, 0.14f, 0.09f)
        mb.appendBox(-0.22f, 0.18f, 0.08f, 0.7f, 0.06f, 0.18f, 0.2f, 0.13f, 0.08f)
        mb.appendSphere(-0.45f, 0.06f, 0.02f, 0.04f, 0.24f, 0.24f, 0.26f, 1f, 6, 7)
        mb.appendSphere(0.42f, 0.06f, -0.02f, 0.04f, 0.24f, 0.24f, 0.26f, 1f, 6, 7)
        return mb.build()
    }

    fun createCoalSmokePlume(): Mesh {
        val mb = MeshBuilder()
        mb.appendSphere(0f, 0.46f, 0f, 0.24f, 0.14f, 0.14f, 0.16f, 1f, 8, 8)
        mb.appendSphere(-0.1f, 0.72f, 0.06f, 0.2f, 0.16f, 0.16f, 0.18f, 1f, 8, 8)
        mb.appendSphere(0.1f, 0.92f, -0.05f, 0.18f, 0.18f, 0.18f, 0.2f, 1f, 8, 8)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    // Ceiling-hung lamp — chain + cage + warm glow bulb
    fun createCeilingLamp(r: Float = 0.98f, g: Float = 0.74f, b: Float = 0.28f): Mesh {
        val mb = MeshBuilder()
        val chainCol = floatArrayOf(0.22f, 0.22f, 0.24f)
        val cageCol  = floatArrayOf(0.18f, 0.14f, 0.10f)
        // Ceiling anchor bracket
        mb.appendBox(0f, 3.96f, 0f, 0.28f, 0.10f, 0.28f, cageCol[0], cageCol[1], cageCol[2])
        // Chain links (segmented cylinders hanging down)
        for (seg in 0 until 6) {
            val y = 3.82f - seg * 0.18f
            mb.appendSphere(0f, y, 0f, 0.04f, chainCol[0], chainCol[1], chainCol[2], 1f, 5, 6)
        }
        mb.appendCylinder(0f, 3.2f, 0f, 0.025f, 0.82f, chainCol[0], chainCol[1], chainCol[2], segments = 6)
        // Cage body
        mb.appendBox(0f, 2.88f, 0f, 0.30f, 0.36f, 0.30f, cageCol[0], cageCol[1], cageCol[2])
        mb.appendBox(0f, 2.88f, 0f, 0.22f, 0.30f, 0.22f, 0.12f, 0.10f, 0.08f)
        // Glow bulb
        mb.appendSphere(0f, 2.84f, 0f, 0.14f, r, g, b, 1.0f, 9, 9)
        mb.appendSphere(0f, 2.84f, 0f, 0.08f, 1f, 0.88f, 0.50f, 1.0f, 8, 8)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    // ── Gameplay obstacles & hazards ──────────────────────────────────────────
    fun createFallenBeam(): Mesh {
        val mb = MeshBuilder()
        val wr = 0.28f; val wg = 0.19f; val wb = 0.12f
        mb.appendBox(0f, 0.14f, 0f, 0.24f, 0.28f, 1.85f, wr, wg, wb)
        mb.appendBox(-0.06f, 0.1f, 0.72f, 0.3f, 0.2f, 0.38f, wr * 0.82f, wg * 0.82f, wb * 0.82f)
        mb.appendSphere(0.1f, 0.08f, -0.62f, 0.06f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        mb.appendSphere(-0.1f, 0.1f, 0.44f, 0.05f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        mb.appendSphere(0f, 0.08f, 0.82f, 0.09f, 0.1f, 0.09f, 0.08f, 1f, 7, 8)
        return mb.build()
    }

    fun createWoodenBarricade(): Mesh {
        val mb = MeshBuilder()
        val wr = 0.27f; val wg = 0.18f; val wb = 0.12f
        mb.appendBox(-0.68f, 0.64f, 0f, 0.18f, 1.28f, 0.18f, wr, wg, wb)
        mb.appendBox(0.68f,  0.64f, 0f, 0.18f, 1.28f, 0.18f, wr, wg, wb)
        mb.appendBox(0f, 0.72f, 0f, 1.28f, 0.14f, 0.14f, wr, wg, wb)
        mb.appendBox(0f, 0.44f, 0f, 1.1f,  0.12f, 0.12f, wr * 0.88f, wg * 0.88f, wb * 0.88f)
        mb.appendBox(0f, 1.0f,  0f, 1.0f,  0.12f, 0.12f, wr, wg, wb)
        mb.appendSphere(-0.48f, 0.75f, 0.07f, 0.03f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        mb.appendSphere(0.52f,  0.5f,  0.07f, 0.03f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        mb.appendSphere(0f, 1.05f, 0.07f, 0.03f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        return mb.build()
    }

    fun createRockfallPile(): Mesh {
        val mb = MeshBuilder()
        val r1 = floatArrayOf(0.11f, 0.12f, 0.13f)
        val r2 = floatArrayOf(0.08f, 0.09f, 0.1f)
        val coal = floatArrayOf(0.04f, 0.04f, 0.05f)
        mb.appendSphere(0f,     0.3f,  0f,    0.62f, r1[0], r1[1], r1[2], 1f, 9, 10)
        mb.appendSphere(0.42f,  0.22f, 0.3f,  0.4f,  r2[0], r2[1], r2[2], 1f, 8, 9)
        mb.appendSphere(-0.46f, 0.2f,  -0.24f, 0.38f, r1[0], r1[1], r1[2], 1f, 8, 9)
        mb.appendSphere(0.22f,  0.42f, -0.32f, 0.32f, r2[0], r2[1], r2[2], 1f, 8, 9)
        mb.appendSphere(-0.2f,  0.18f, 0.42f, 0.28f, coal[0], coal[1], coal[2], 1f, 8, 9)
        mb.appendSphere(0.52f,  0.14f, -0.12f, 0.24f, r1[0], r1[1], r1[2], 1f, 7, 8)
        mb.appendSphere(-0.32f, 0.14f, -0.52f, 0.2f, coal[0], coal[1], coal[2], 1f, 7, 8)
        return mb.build()
    }

    fun createCrateStack(): Mesh {
        val mb = MeshBuilder()
        val wr = 0.24f; val wg = 0.17f; val wb = 0.11f
        val pl = floatArrayOf(0.19f, 0.13f, 0.08f)
        mb.appendBox(0f, 0.3f,   0f,    0.74f, 0.6f, 0.66f, wr, wg, wb)
        mb.appendBox(0f, 0.3f,   0f,    0.68f, 0.5f, 0.6f,  pl[0], pl[1], pl[2])
        mb.appendBox(0f, 0.3f,   0f,    0.72f, 0.06f, 0.64f, 0.16f, 0.11f, 0.07f)
        mb.appendBox(0f, 0.3f,   0f,    0.06f, 0.5f, 0.62f,  0.16f, 0.11f, 0.07f)
        mb.appendBox(0f, 0.86f,  0.06f, 0.6f,  0.5f, 0.54f,  wr, wg, wb)
        mb.appendBox(0f, 0.86f,  0.06f, 0.54f, 0.42f, 0.48f, pl[0], pl[1], pl[2])
        mb.appendBox(0f, 0.86f,  0.06f, 0.58f, 0.06f, 0.52f, 0.16f, 0.11f, 0.07f)
        mb.appendSphere(-0.32f, 0.6f, 0.3f, 0.03f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        mb.appendSphere(0.32f,  0.6f, 0.3f, 0.03f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        return mb.build()
    }

    fun createSpikeTrap(): Mesh {
        val mb = MeshBuilder()
        val wr = 0.23f; val wg = 0.16f; val wb = 0.1f
        mb.appendBox(0f, 0.04f, 0f, 1.22f, 0.08f, 0.34f, wr, wg, wb)
        for (xi in 0 until 5) {
            val x = -0.44f + xi * 0.22f
            mb.appendCone(x, 0.28f, 0f, 0.045f, 0.4f, wr, wg, wb, 1f, 6)
        }
        mb.appendSphere(-0.58f, 0.04f, 0.14f, 0.03f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        mb.appendSphere(0.58f,  0.04f, -0.14f, 0.03f, 0.22f, 0.22f, 0.24f, 1f, 6, 7)
        return mb.build()
    }

    fun createCollapsedSupportDebris(): Mesh {
        val mb = MeshBuilder()
        val wr = 0.27f; val wg = 0.18f; val wb = 0.12f
        val rock = floatArrayOf(0.1f, 0.11f, 0.12f)
        mb.appendBox(0f, 0.14f, 0f, 0.26f, 0.28f, 1.72f, wr, wg, wb)
        mb.appendBox(-0.36f, 0.2f, 0.52f, 0.28f, 0.38f, 0.56f, wr * 0.84f, wg * 0.84f, wb * 0.84f)
        mb.appendBox(0f, 0.36f, -0.32f, 0.24f, 0.72f, 0.26f, wr, wg, wb)
        mb.appendSphere(0.22f,  0.24f, -0.46f, 0.28f, rock[0], rock[1], rock[2], 1f, 8, 9)
        mb.appendSphere(-0.18f, 0.22f, 0.32f,  0.24f, rock[0], rock[1], rock[2], 1f, 8, 9)
        mb.appendSphere(0.1f,   0.16f, 0.62f,  0.2f,  0.04f, 0.04f, 0.05f, 1f, 7, 8)
        mb.appendSphere(-0.28f, 0.14f, -0.22f, 0.16f, rock[0], rock[1], rock[2], 1f, 7, 8)
        return mb.build()
    }

    // ── Ceiling section: hanging rocks, chains, wooden cross-bracing ──────────
    fun createCaveCeilingSection(ceilY: Float = 4.2f): Mesh {
        val mb = MeshBuilder()
        val rock1 = floatArrayOf(0.1f, 0.11f, 0.12f)
        val rock2 = floatArrayOf(0.08f, 0.09f, 0.1f)
        val wood  = floatArrayOf(0.3f, 0.21f, 0.14f)
        val metal = floatArrayOf(0.2f, 0.2f, 0.22f)

        // Wooden cross-brace beams spanning the ceiling width
        mb.appendBox(0f, ceilY - 0.08f, 0f,      5.2f, 0.18f, 0.22f, wood[0], wood[1], wood[2])
        mb.appendBox(0f, ceilY - 0.08f, 0.5f,    5.0f, 0.16f, 0.2f,  wood[0] * 0.9f, wood[1] * 0.9f, wood[2] * 0.9f)
        mb.appendBox(0f, ceilY - 0.34f, 0.22f,   0.2f, 0.52f, 4.8f,  wood[0], wood[1], wood[2])

        // Hanging chains from ceiling to cross-beam
        for (x in floatArrayOf(-2.4f, 0f, 2.4f)) {
            for (seg in 0 until 5) {
                val y = ceilY - 0.3f - seg * 0.14f
                mb.appendSphere(x, y, 0.08f, 0.04f, metal[0], metal[1], metal[2], 1f, 6, 7)
            }
        }

        // Hanging rock formations / stalactite clusters
        mb.appendCone(-1.6f, ceilY - 0.72f, -0.3f, 0.09f, 0.34f, rock2[0], rock2[1], rock2[2], 1f, 7)
        mb.appendCone(1.4f,  ceilY - 0.8f,   0.4f, 0.07f, 0.42f, rock1[0], rock1[1], rock1[2], 1f, 7)
        mb.appendCone(-0.5f, ceilY - 0.65f,  0.18f, 0.11f, 0.28f, rock2[0], rock2[1], rock2[2], 1f, 8)
        mb.appendCone(0.8f,  ceilY - 0.88f, -0.42f, 0.08f, 0.48f, rock1[0], rock1[1], rock1[2], 1f, 7)
        mb.appendCone(-2.0f, ceilY - 0.7f,   0.06f, 0.1f,  0.36f, rock2[0], rock2[1], rock2[2], 1f, 8)
        mb.appendCone(2.1f,  ceilY - 0.62f, -0.22f, 0.09f, 0.3f,  rock1[0], rock1[1], rock1[2], 1f, 7)

        // Irregular ceiling rock chunks
        mb.appendSphere(-1.2f, ceilY - 0.36f, -0.2f, 0.28f, rock1[0], rock1[1], rock1[2], 1f, 8, 9)
        mb.appendSphere(1.5f,  ceilY - 0.42f,  0.3f, 0.24f, rock2[0], rock2[1], rock2[2], 1f, 8, 9)
        mb.appendSphere(0.1f,  ceilY - 0.38f, -0.4f, 0.22f, rock1[0], rock1[1], rock1[2], 1f, 8, 9)

        // Coal staining on ceiling surface
        mb.appendBox(-0.8f, ceilY - 0.14f, 0.12f, 0.9f, 0.12f, 1.4f, 0.04f, 0.04f, 0.05f)
        mb.appendBox(1.1f,  ceilY - 0.14f, -0.2f, 0.7f, 0.12f, 1.1f, 0.04f, 0.04f, 0.05f)

        return mb.build()
    }

    // Floor-mounted lantern on a pole with warm glow sphere
    fun createLanternStand(r: Float = 0.98f, g: Float = 0.74f, b: Float = 0.28f): Mesh {
        val mb = MeshBuilder()
        mb.appendCylinder(0f, 0.62f, 0f, 0.04f, 1.24f, 0.18f, 0.14f, 0.12f, segments = 7)
        mb.appendBox(0f, 0.08f, 0f, 0.3f, 0.12f, 0.3f, 0.14f, 0.12f, 0.1f)
        mb.appendBox(0f, 1.18f, 0f, 0.22f, 0.08f, 0.22f, 0.16f, 0.12f, 0.1f)
        mb.appendBox(0f, 1.1f, 0f, 0.15f, 0.38f, 0.15f, 0.2f, 0.16f, 0.12f)
        mb.appendSphere(0f, 1.04f, 0f, 0.14f, r, g, b, 1.0f, 9, 9)
        mb.appendSphere(0f, 1.04f, 0f, 0.07f, 1f, 0.9f, 0.5f, 1.0f, 8, 8)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    // Crossed warning plank sign nailed to a post
    fun createWarningSign(): Mesh {
        val mb = MeshBuilder()
        val wood = floatArrayOf(0.28f, 0.2f, 0.13f)
        mb.appendCylinder(0f, 0.6f, 0f, 0.04f, 1.2f, wood[0], wood[1], wood[2], segments = 7)
        mb.appendBox(0f, 1.14f, 0f, 0.58f, 0.32f, 0.07f, 0.28f, 0.22f, 0.14f)
        mb.appendBox(-0.18f, 1.08f, 0.04f, 0.38f, 0.06f, 0.06f, 0.26f, 0.2f, 0.12f)
        mb.appendBox(0.18f, 1.2f, 0.04f, 0.38f, 0.06f, 0.06f, 0.26f, 0.2f, 0.12f)
        // Orange-yellow warning cross mark
        mb.appendBox(0f, 1.14f, 0.04f, 0.06f, 0.22f, 0.02f, 0.92f, 0.56f, 0.1f)
        mb.appendBox(0f, 1.14f, 0.04f, 0.22f, 0.06f, 0.02f, 0.92f, 0.56f, 0.1f)
        return mb.build()
    }

    // Coiled rope bundle on the ground
    fun createRopeBundle(): Mesh {
        val mb = MeshBuilder()
        mb.appendCylinder(0f, 0.06f, 0f, 0.22f, 0.12f, 0.36f, 0.28f, 0.18f, segments = 10)
        mb.appendCylinder(0f, 0.1f, 0f, 0.12f, 0.12f, 0.28f, 0.22f, 0.14f, segments = 10)
        mb.appendCylinder(0.28f, 0.05f, 0.1f, 0.08f, 0.08f, 0.34f, 0.26f, 0.17f, segments = 8)
        mb.appendSphere(0.14f, 0.09f, -0.16f, 0.06f, 0.32f, 0.24f, 0.16f, 1f, 6, 7)
        return mb.build()
    }

    // Simple wooden excavation platform shelf
    fun createWoodenPlatform(): Mesh {
        val mb = MeshBuilder()
        val wood = floatArrayOf(0.3f, 0.2f, 0.13f)
        mb.appendBox(0f, 0.72f, 0f, 1.85f, 0.1f, 0.72f, wood[0], wood[1], wood[2])
        mb.appendBox(-0.82f, 0.36f, 0f, 0.12f, 0.72f, 0.62f, wood[0] * 0.9f, wood[1] * 0.9f, wood[2] * 0.9f)
        mb.appendBox(0.82f,  0.36f, 0f, 0.12f, 0.72f, 0.62f, wood[0] * 0.9f, wood[1] * 0.9f, wood[2] * 0.9f)
        mb.appendBox(0f, 0.72f, 0f, 1.72f, 0.06f, 0.6f, 0.24f, 0.16f, 0.1f)
        mb.appendBox(-0.56f, 0.75f, 0.02f, 0.52f, 0.06f, 0.6f, 0.22f, 0.15f, 0.09f)
        mb.appendBox(0.24f, 0.77f, -0.02f, 0.48f, 0.06f, 0.6f, 0.22f, 0.15f, 0.09f)
        mb.appendSphere(-0.82f, 0.76f, 0.28f, 0.04f, 0.24f, 0.24f, 0.26f, 1f, 6, 7)
        mb.appendSphere(0.82f,  0.76f, -0.28f, 0.04f, 0.24f, 0.24f, 0.26f, 1f, 6, 7)
        return mb.build()
    }

    fun createIceCrystalCluster(): Mesh {
        val mb = MeshBuilder()
        val iceA = floatArrayOf(0.38f, 0.62f, 0.86f)
        val iceB = floatArrayOf(0.5f, 0.78f, 0.96f)
        val iceC = floatArrayOf(0.3f, 0.54f, 0.8f)

        mb.appendCone(0f, 0.42f, 0f, 0.12f, 0.82f, iceB[0], iceB[1], iceB[2], 1f, 8)
        mb.appendCone(-0.22f, 0.34f, 0.08f, 0.09f, 0.66f, iceA[0], iceA[1], iceA[2], 1f, 8)
        mb.appendCone(0.2f, 0.3f, -0.06f, 0.08f, 0.58f, iceC[0], iceC[1], iceC[2], 1f, 8)
        mb.appendCone(0.06f, 0.25f, 0.2f, 0.07f, 0.48f, iceA[0], iceA[1], iceA[2], 1f, 8)
        mb.appendSphere(0f, 0.06f, 0f, 0.22f, 0.1f, 0.14f, 0.2f, 1f, 8, 9)
        mb.appendSphere(-0.18f, 0.04f, 0.12f, 0.14f, 0.08f, 0.11f, 0.16f, 1f, 7, 8)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    fun createCorruptedCrystalCluster(): Mesh {
        val mb = MeshBuilder()
        val cA = floatArrayOf(0.16f, 0.38f, 0.86f)
        val cB = floatArrayOf(0.24f, 0.52f, 0.98f)
        val cC = floatArrayOf(0.1f, 0.3f, 0.7f)
        mb.appendCone(0f, 0.56f, 0f, 0.13f, 1.08f, cB[0], cB[1], cB[2], 1f, 8)
        mb.appendCone(-0.26f, 0.4f, 0.16f, 0.09f, 0.8f, cA[0], cA[1], cA[2], 1f, 8)
        mb.appendCone(0.22f, 0.36f, -0.12f, 0.08f, 0.72f, cC[0], cC[1], cC[2], 1f, 8)
        mb.appendCone(0.1f, 0.3f, 0.24f, 0.08f, 0.62f, cA[0], cA[1], cA[2], 1f, 8)
        mb.appendSphere(0f, 0.08f, 0f, 0.24f, 0.05f, 0.08f, 0.14f, 1f, 8, 9)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    fun createSnowDriftPatch(): Mesh {
        val mb = MeshBuilder()
        mb.appendSphere(0f, 0.1f, 0f, 0.62f, 0.84f, 0.9f, 0.97f, 1f, 9, 10)
        mb.appendSphere(-0.38f, 0.08f, 0.22f, 0.42f, 0.82f, 0.88f, 0.95f, 1f, 8, 9)
        mb.appendSphere(0.42f, 0.07f, -0.2f, 0.38f, 0.8f, 0.86f, 0.94f, 1f, 8, 9)
        mb.appendSphere(0.12f, 0.16f, 0.06f, 0.26f, 0.9f, 0.94f, 0.98f, 1f, 8, 9)
        return mb.build()
    }

    fun createFrozenPuddlePatch(): Mesh {
        val mb = MeshBuilder()
        mb.appendBox(0f, 0.01f, 0f, 1.2f, 0.02f, 0.92f, 0.48f, 0.66f, 0.82f, 1f)
        mb.appendBox(0.18f, 0.015f, 0.24f, 0.42f, 0.02f, 0.24f, 0.68f, 0.82f, 0.94f, 1f)
        mb.appendBox(-0.2f, 0.015f, -0.18f, 0.34f, 0.02f, 0.2f, 0.62f, 0.78f, 0.9f, 1f)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    fun createIceMountainFacade(): Mesh {
        val mb = MeshBuilder()
        val iceA = floatArrayOf(0.28f, 0.44f, 0.64f)
        val iceB = floatArrayOf(0.38f, 0.56f, 0.76f)
        mb.appendCone(0f, 2.1f, 0f, 2.4f, 4.4f, iceA[0], iceA[1], iceA[2], 1f, 12)
        mb.appendCone(-1.6f, 1.6f, 0.7f, 1.5f, 3.2f, iceB[0], iceB[1], iceB[2], 1f, 10)
        mb.appendCone(1.7f, 1.5f, -0.8f, 1.6f, 3.0f, iceB[0], iceB[1], iceB[2], 1f, 10)
        mb.appendBox(0f, 0.36f, 0f, 5.8f, 0.72f, 3.2f, 0.32f, 0.44f, 0.6f)
        mb.appendBox(0f, 1.1f, 0f, 2.2f, 1.6f, 1.2f, 0.14f, 0.18f, 0.24f)
        return mb.build()
    }

    fun createMineEntranceSignAku(): Mesh {
        val mb = MeshBuilder()
        val wood = floatArrayOf(0.29f, 0.2f, 0.13f)
        mb.appendCylinder(-1.25f, 0.86f, 0f, 0.08f, 1.72f, wood[0], wood[1], wood[2], segments = 7)
        mb.appendCylinder(1.25f, 0.86f, 0f, 0.08f, 1.72f, wood[0], wood[1], wood[2], segments = 7)
        mb.appendBox(0f, 1.42f, 0f, 2.9f, 0.88f, 0.12f, 0.24f, 0.17f, 0.11f)
        mb.appendBox(0f, 1.42f, 0.06f, 2.74f, 0.74f, 0.04f, 0.18f, 0.13f, 0.09f)
        // Carved Aku mark and stylized sign strokes
        mb.appendCone(0f, 1.48f, 0.08f, 0.22f, 0.24f, 0.08f, 0.04f, 0.04f, 1f, 7)
        mb.appendBox(0f, 1.36f, 0.08f, 0.62f, 0.05f, 0.02f, 0.08f, 0.04f, 0.04f)
        mb.appendBox(-0.62f, 1.58f, 0.08f, 0.34f, 0.05f, 0.02f, 0.08f, 0.04f, 0.04f)
        mb.appendBox(0.62f, 1.58f, 0.08f, 0.34f, 0.05f, 0.02f, 0.08f, 0.04f, 0.04f)
        return mb.build()
    }

    fun createIcicleHazardCluster(): Mesh {
        val mb = MeshBuilder()
        val ice = floatArrayOf(0.56f, 0.78f, 0.95f)
        mb.appendCone(0f, 1.62f, 0f, 0.08f, 0.92f, ice[0], ice[1], ice[2], 1f, 8)
        mb.appendCone(-0.24f, 1.68f, 0.14f, 0.06f, 0.74f, ice[0], ice[1], ice[2], 1f, 8)
        mb.appendCone(0.2f, 1.72f, -0.12f, 0.05f, 0.62f, ice[0], ice[1], ice[2], 1f, 8)
        val mesh = mb.build()
        mesh.silhouetteMode = 2
        return mesh
    }

    private fun mergeMeshes(first: Mesh, second: Mesh): Mesh {
        first.vertexBuffer.position(0)
        second.vertexBuffer.position(0)
        first.normalBuffer.position(0)
        second.normalBuffer.position(0)
        first.colorBuffer.position(0)
        second.colorBuffer.position(0)
        first.indexBuffer.position(0)
        second.indexBuffer.position(0)

        val firstVertices = FloatArray(first.vertexBuffer.remaining()).also { first.vertexBuffer.get(it) }
        val secondVertices = FloatArray(second.vertexBuffer.remaining()).also { second.vertexBuffer.get(it) }
        val firstNormals = FloatArray(first.normalBuffer.remaining()).also { first.normalBuffer.get(it) }
        val secondNormals = FloatArray(second.normalBuffer.remaining()).also { second.normalBuffer.get(it) }
        val firstColors = FloatArray(first.colorBuffer.remaining()).also { first.colorBuffer.get(it) }
        val secondColors = FloatArray(second.colorBuffer.remaining()).also { second.colorBuffer.get(it) }
        val firstIndices = ShortArray(first.indexBuffer.remaining()).also { first.indexBuffer.get(it) }
        val secondIndicesRaw = ShortArray(second.indexBuffer.remaining()).also { second.indexBuffer.get(it) }

        val firstVertexCount = (firstVertices.size / 3).toShort()
        val mergedIndices = ShortArray(firstIndices.size + secondIndicesRaw.size)
        System.arraycopy(firstIndices, 0, mergedIndices, 0, firstIndices.size)
        for (i in secondIndicesRaw.indices) {
            mergedIndices[firstIndices.size + i] = (secondIndicesRaw[i] + firstVertexCount).toShort()
        }

        val merged = Mesh()
        merged.initBuffers(
            firstVertices + secondVertices,
            firstNormals + secondNormals,
            firstColors + secondColors,
            mergedIndices
        )
        return merged
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
