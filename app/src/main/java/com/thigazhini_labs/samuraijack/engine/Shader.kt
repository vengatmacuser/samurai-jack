package com.thigazhini_labs.samuraijack.engine

import android.opengl.GLES30
import android.util.Log

class Shader {
    companion object {
        private const val TAG = "Shader"

        const val VERTEX_SHADER_CODE = """#version 300 es
            layout(location = 0) in vec3 vPosition;
            layout(location = 1) in vec3 vNormal;
            layout(location = 2) in vec4 vColor;
            layout(location = 3) in vec2 vTexCoord;

            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform float uAnimTime;

            out vec3 fPosition;
            out vec3 fNormal;
            out vec4 fColor;
            out vec2 fTexCoord;

            void main() {
                vec3 pos = vPosition;
                if (uAnimTime > 0.0) {
                    // Simulate leg swing for vertices below waist in model space
                    if (pos.y < 0.35) {
                        float swing = sin(uAnimTime) * 0.18 * (0.35 - pos.y);
                        if (pos.x > 0.0) {
                            pos.z += swing;
                        } else {
                            pos.z -= swing;
                        }
                    }
                    // Slight body bounce
                    pos.y += abs(sin(uAnimTime)) * 0.04;
                }

                gl_Position = uMVPMatrix * vec4(pos, 1.0);
                fPosition = vec3(uModelMatrix * vec4(pos, 1.0));
                
                // Normal transformed to world space (assuming uniform scaling)
                fNormal = normalize(vec3(uModelMatrix * vec4(vNormal, 0.0)));
                fColor = vColor;
                fTexCoord = vTexCoord;
            }
        """

        const val FRAGMENT_SHADER_CODE = """#version 300 es
            precision mediump float;

            in vec3 fPosition;
            in vec3 fNormal;
            in vec4 fColor;
            in vec2 fTexCoord;

            // Camera & Fog parameters
            uniform vec3 uCameraPos;
            uniform vec3 uFogColor;
            uniform float uFogDensity;

            // Dynamic Lights
            uniform vec3 uDirLightDir;      // Directional light direction
            uniform vec3 uDirLightColor;    // Directional light color
            uniform vec3 uPointLightPos;    // Point light position
            uniform vec3 uPointLightColor;  // Point light color
            uniform float uPointLightIntensity;

            // Visual effects uniforms
            uniform int uSilhouetteMode;    // 0 = Normal, 1 = Black silhouette (Jack), 2 = Glow eyes/blade, 3 = Textured background, 4 = Textured character
            uniform float uHitFlashRed;     // 0.0 = normal, 1.0 = full red flash (damage)

            uniform sampler2D uTexture;

            out vec4 fragColor;

            void main() {
                // 1. Silhouette / Texturing Logic
                if (uSilhouetteMode == 3) {
                    vec4 texColor = texture(uTexture, fTexCoord);
                    // Apply exponential fog to the background textured backdrop
                    float fragmentDist = distance(uCameraPos, fPosition);
                    float fogFactor = exp(-uFogDensity * fragmentDist);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    fragColor = vec4(mix(uFogColor, texColor.rgb, fogFactor), texColor.a);
                    return;
                }

                if (uSilhouetteMode == 1) {
                    // Render character as pure dark silhouette (Cartoon Network signature)
                    fragColor = vec4(0.08, 0.08, 0.1, 1.0);
                    return;
                } else if (uSilhouetteMode == 2) {
                    // Render glowing parts (sword / eyes)
                    fragColor = fColor;
                    return;
                }

                vec4 baseColor = fColor;
                if (uSilhouetteMode == 4) {
                    baseColor = texture(uTexture, fTexCoord);
                }

                vec3 normal = normalize(fNormal);
                vec3 viewDir = normalize(uCameraPos - fPosition);

                // 2. Cel-Shading (Toon) Diffuse Factor
                vec3 lightDir = normalize(-uDirLightDir);
                float diff = dot(normal, lightDir);
                
                // Quantize diffuse lighting bands for Cartoon style (enhanced shadows)
                float celDiffuse;
                if (diff > 0.5) {
                    celDiffuse = 1.0;
                } else if (diff > -0.1) {
                    celDiffuse = 0.45;
                } else {
                    celDiffuse = 0.08; // Darker ambient contribution for stronger shadow contrast
                }

                // Directional light contribution
                vec3 dirDiffuseColor = uDirLightColor * celDiffuse;

                // 3. Point Light Contribution (for neon robot eyes, sparks, lasers)
                vec3 pointLightDir = uPointLightPos - fPosition;
                float dist = length(pointLightDir);
                pointLightDir = normalize(pointLightDir);

                float pointDiff = max(dot(normal, pointLightDir), 0.0);
                float attenuation = 1.0 / (1.0 + 0.1 * dist + 0.05 * dist * dist);
                
                // Quantize point light diffuse
                float celPoint;
                if (pointDiff > 0.5) {
                    celPoint = 1.0;
                } else if (pointDiff > 0.1) {
                    celPoint = 0.4;
                } else {
                    celPoint = 0.0;
                }
                
                vec3 pointDiffuseColor = uPointLightColor * celPoint * attenuation * uPointLightIntensity;

                // Combine lights
                vec3 finalLight = dirDiffuseColor + pointDiffuseColor;
                vec4 litColor = vec4(baseColor.rgb * finalLight, baseColor.a);

                // 4. Hit Flash Damage overlay
                if (uHitFlashRed > 0.0) {
                    litColor.rgb = mix(litColor.rgb, vec3(0.9, 0.1, 0.1), uHitFlashRed);
                }

                // 5. Exponential Fog (UE5 atmospheric feel)
                float fragmentDist = distance(uCameraPos, fPosition);
                float fogFactor = exp(-uFogDensity * fragmentDist);
                fogFactor = clamp(fogFactor, 0.0, 1.0);

                fragColor = vec4(mix(uFogColor, litColor.rgb, fogFactor), litColor.a);
            }
        """

        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader))
                GLES30.glDeleteShader(shader)
                return 0
            }
            return shader
        }

        fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error linking program: " + GLES30.glGetProgramInfoLog(program))
                GLES30.glDeleteProgram(program)
                return 0
            }
            return program
        }
    }
}
