package imgui.impl

import gli_.gl
import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4i
import gln.*
import gln.glf.semantic
import gln.objects.GlBuffers
import gln.objects.GlProgram
import gln.objects.GlShader
import gln.objects.GlTexture
import gln.texture.TexFilter
import gln.uniform.glUniform
import gln.vertexArray.GlVertexArray
import gln.vertexArray.glVertexAttribPointer
import imgui.*
import kool.ByteBuffer
import kool.IntBuffer
import kool.free
import kool.set

import org.lwjgl.opengl.GL43C.*
import org.lwjgl.opengl.GL45C

class ImplGL43 : LwjglRendererI {
    var program = GlProgram(0)
    var matUL = -1
    var CLIP_ORIGIN = false

    val buffers = GlBuffers<Buffer>()
    var vao = GlVertexArray()
    var fontTexture = GlTexture()

    override fun createDeviceObjects(): Boolean {
        ImGui.io.backendRendererName = "imgui impl opengl3"

        // Backup GL state
        // we have to save also program since we do the uniform mat and texture setup once here
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)

        program = GlProgram.create().apply {
            val vertHandle = GlShader.createFromSource(ShaderType.VERTEX_SHADER, vertexShader)
            val fragHandle = GlShader.createFromSource(ShaderType.FRAGMENT_SHADER, fragmentShader)
            attach(vertHandle)
            attach(fragHandle)
            bindAttribLocation(semantic.attr.POSITION, "Position")
            bindAttribLocation(semantic.attr.TEX_COORD, "UV")
            bindAttribLocation(semantic.attr.COLOR, "Color")
            bindFragDataLocation(semantic.frag.COLOR, "outColor")
            link()
            detach(vertHandle)
            detach(fragHandle)
            vertHandle.delete()
            fragHandle.delete()
        }
        program.use {
            matUL = "mat".uniform
            "Texture".unit = semantic.sampler.DIFFUSE
        }

        GlVertexArray.gen(::vao).bound {

            buffers.gen {

                Buffer.Vertex.bound(BufferTarget.ARRAY) {

                    data(vtxSize, Usage.STREAM_DRAW)

                    glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
                    glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
                    glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)
                }
                glEnableVertexAttribArray(semantic.attr.POSITION)
                glEnableVertexAttribArray(semantic.attr.TEX_COORD)
                glEnableVertexAttribArray(semantic.attr.COLOR)

                Buffer.Element.bind(BufferTarget.ELEMENT_ARRAY) {
                    data(idxSize, Usage.STREAM_DRAW)
                }
            }
        }

        createFontsTexture()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)
        glBindVertexArray(lastVertexArray)

        return checkError("createDeviceObject")
    }

    private fun createFontsTexture(): Boolean {
        if (ImGui.io.fonts.isBuilt)
            return true

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = ImGui.io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        GlTexture.gen(::fontTexture).bound(TextureTarget._2D) {
            minMagFilter = TexFilter.LINEAR
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            image(gl.InternalFormat.RGBA8_UNORM, size, gl.ExternalFormat.RGBA, gl.TypeFormat.U8, pixels)
        }

        // Store our identifier
        ImGui.io.fonts.texId = fontTexture.name

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return checkError("createFontsTexture")
    }

    override fun renderDrawData(drawData: DrawData) {
        val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
        val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
        if (fbWidth == 0 || fbHeight == 0) return

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastSampler = glGetInteger(GL_SAMPLER_BINDING)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
        val lastPolygonMode = glGetInteger(GL_POLYGON_MODE)
        val lastViewport = glGetVec4i(GL_VIEWPORT)
        val lastScissorBox = glGetVec4i(GL_SCISSOR_BOX)
        val lastBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        val lastBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        val lastBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        val lastBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        val lastBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
        val lastBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
        val lastEnableBlend = glIsEnabled(GL_BLEND)
        val lastEnableCullFace = glIsEnabled(GL_CULL_FACE)
        val lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
        val lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)
        val clipOriginLowerLeft = when {
            CLIP_ORIGIN && glGetInteger(GL45C.GL_CLIP_ORIGIN) == GL_UPPER_LEFT -> false // Support for GL 4.5's glClipControl(GL_UPPER_LEFT)
            else -> true
        }

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        glEnable(GL_BLEND)
        glBlendEquation(GL_FUNC_ADD)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_SCISSOR_TEST)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

        // Setup viewport, orthographic projection matrix
        glViewport(0, 0, fbWidth, fbHeight)
        val ortho = glm.ortho(mat, 0f, ImGui.io.displaySize.x.f, ImGui.io.displaySize.y.f, 0f)
        glUseProgram(program.i)
        glUniform(matUL, ortho)

        checkSize(drawData.cmdLists)

        vao.bind()
        glBindSampler(semantic.sampler.DIFFUSE, 0) // Rely on combined texture/sampler state.

        // Will project scissor/clipping rectangles into framebuffer space
        val clipOff = drawData.displayPos     // (0,0) unless using multi-viewports
        val clipScale = drawData.framebufferScale   // (1,1) unless using retina display which are often (2,2)

        for (cmdList in drawData.cmdLists) {

            cmdList.vtxBuffer.forEachIndexed { i, v ->
                val offset = i * DrawVert.size
                v.pos.to(vtxBuffer, offset)
                v.uv.to(vtxBuffer, offset + Vec2.size)
                vtxBuffer.putInt(offset + Vec2.size * 2, v.col)
            }
            buffers {
                Buffer.Vertex.bind(BufferTarget.ARRAY) {
                    subData(0, cmdList._vtxWritePtr * DrawVert.size, vtxBuffer)
                }
                cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
                Buffer.Element.bind(BufferTarget.ELEMENT_ARRAY) {
                    subData(0, cmdList._idxWritePtr * Int.BYTES, idxBuffer)
                }
            }
            var idxBufferOffset = 0L
            val l = HashMap<TextureID, ArrayList<Pair<Vec4i, IntArray>>>()
            for (cmd in cmdList.cmdBuffer) {
                val cb = cmd.userCallback
                if (cb != null)
                // User callback (registered via ImDrawList::AddCallback)
                    cb(cmdList, cmd)
                else {
                    // Project scissor/clipping rectangles into framebuffer space
                    val clipRectX = (cmd.clipRect.x - clipOff.x) * clipScale.x
                    val clipRectY = (cmd.clipRect.y - clipOff.y) * clipScale.y
                    val clipRectZ = (cmd.clipRect.z - clipOff.x) * clipScale.x
                    val clipRectW = (cmd.clipRect.w - clipOff.y) * clipScale.y

                    if (clipRectX < fbWidth && clipRectY < fbHeight && clipRectZ >= 0f && clipRectW >= 0f) {
                        // Apply scissor/clipping rectangle
                        val scissor = if (clipOriginLowerLeft)
                            Vec4i(clipRectX.i, (fbHeight - clipRectW).i, (clipRectZ - clipRectX).i, (clipRectW - clipRectY).i)
                        else
                            Vec4i(clipRectX.i, clipRectY.i, clipRectZ.i, clipRectW.i) // Support for GL 4.5's glClipControl(GL_UPPER_LEFT)

                        // Bind texture, Draw
                        val pair = Pair(scissor, intArrayOf(cmdList.idxBuffer.size, 1, 0, idxBufferOffset.toInt(), 0))
                        if (l.containsKey(cmd.textureId!!)) {
                            l[cmd.textureId!!]!!.add(pair)
                        } else {
                            l[cmd.textureId!!] = arrayListOf(pair)
                        }
                        //glBindTexture(GL_TEXTURE_2D, cmd.textureId!!)
                        //glScissor(scissor)
                        //glDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, intArrayOf(cmdList.idxBuffer.size, 1, 0, idxBufferOffset.toInt(), 0))
                    }
                }
                idxBufferOffset += cmd.elemCount * Int.BYTES
            }
            for((texID, commandPair) in l) {
                glBindTexture(GL_TEXTURE_2D, texID)
                var fCmd = intArrayOf()
                val finalScissor = Vec4i(commandPair[0].first)
                commandPair.forEach { (scissor, command) ->
                    finalScissor.x = finalScissor.x min scissor.x
                    finalScissor.y = finalScissor.y min scissor.y
                    finalScissor.z = finalScissor.z max scissor.z
                    finalScissor.w = finalScissor.w max scissor.w
                    fCmd += command
                }
                glScissor(finalScissor)
                glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, fCmd, commandPair.size, 0)
            }
        }

        checkError("render")

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindSampler(0, lastSampler)
        glActiveTexture(lastActiveTexture)
        glBindVertexArray(lastVertexArray)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)
        glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha)
        glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha)
        if (lastEnableBlend) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        if (lastEnableCullFace) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
        if (lastEnableDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        if (lastEnableScissorTest) glEnable(GL_SCISSOR_TEST) else glDisable(GL_SCISSOR_TEST)
        glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode)
        glViewport(lastViewport)
        glScissor(lastScissorBox)
    }

    private fun checkSize(draws: ArrayList<DrawList>) {

        val minVtxSize = draws.map { it.vtxBuffer.size }.sum() * DrawVert.size
        val minIdxSize = draws.map { it.idxBuffer.size }.sum() * Int.BYTES

        var newVtxSize = vtxSize
        while (newVtxSize < minVtxSize)
            newVtxSize = newVtxSize shl 1
        var newIdxSize = idxSize
        while (newIdxSize < minIdxSize)
            newIdxSize = newIdxSize shl 1

        if (newVtxSize != vtxSize || newIdxSize != idxSize) {

            vtxSize = newVtxSize
            idxSize = newIdxSize

            vtxBuffer.free()
            vtxBuffer = ByteBuffer(vtxSize)
            idxBuffer.free()
            idxBuffer = IntBuffer(idxSize / Int.BYTES)

            val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
            val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
            val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)

            vao.bound {

                buffers {

                    Buffer.Vertex.bound(BufferTarget.ARRAY) {

                        data(vtxSize, Usage.STREAM_DRAW)

                        glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
                        glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
                        glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)
                    }
                    glEnableVertexAttribArray(semantic.attr.POSITION)
                    glEnableVertexAttribArray(semantic.attr.TEX_COORD)
                    glEnableVertexAttribArray(semantic.attr.COLOR)

                    Buffer.Element.bind(BufferTarget.ELEMENT_ARRAY) {
                        data(idxSize, Usage.STREAM_DRAW)
                    }
                }
            }

            glBindVertexArray(lastVertexArray)
            glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)

            checkError("checkSize")

            if (DEBUG) println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
        }
    }

    override fun destroyDeviceObjects() {

        vao.delete()
        buffers.delete()

        if (program.isValid) program.delete()

        destroyFontsTexture()
    }

    private fun destroyFontsTexture() {
        if (fontTexture.isValid) {
            fontTexture.delete()
            ImGui.io.fonts.texId = 0
            fontTexture = GlTexture()
        }
    }
}