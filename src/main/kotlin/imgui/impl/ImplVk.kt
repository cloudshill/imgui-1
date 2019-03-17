package imgui.impl

import glm_.*
import glm_.vec4.Vec4
import imgui.ImGui.io
import imgui.DrawData
import imgui.DrawIdx
import imgui.DrawVert
import kool.toBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import vkk.*
import vkk.entities.*
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO
import vkk.extensionFunctions.destroyImageView
import vkk.extensionFunctions.getSurfaceCapabilitiesKHR
import vkk.extensionFunctions.memoryProperties
import vkk.extensionFunctions.waitIdle


/** glsl_shader.vert, compiled with:
 *  # glslangValidator -V -x -o glsl_shader.vert.u32 glsl_shader.vert
 */
val glslShaderVertSpv = intArrayOf(
        0x07230203, 0x00010000, 0x00080001, 0x0000002e, 0x00000000, 0x00020011, 0x00000001, 0x0006000b,
        0x00000001, 0x4c534c47, 0x6474732e, 0x3035342e, 0x00000000, 0x0003000e, 0x00000000, 0x00000001,
        0x000a000f, 0x00000000, 0x00000004, 0x6e69616d, 0x00000000, 0x0000000b, 0x0000000f, 0x00000015,
        0x0000001b, 0x0000001c, 0x00030003, 0x00000002, 0x000001c2, 0x00040005, 0x00000004, 0x6e69616d,
        0x00000000, 0x00030005, 0x00000009, 0x00000000, 0x00050006, 0x00000009, 0x00000000, 0x6f6c6f43,
        0x00000072, 0x00040006, 0x00000009, 0x00000001, 0x00005655, 0x00030005, 0x0000000b, 0x0074754f,
        0x00040005, 0x0000000f, 0x6c6f4361, 0x0000726f, 0x00030005, 0x00000015, 0x00565561, 0x00060005,
        0x00000019, 0x505f6c67, 0x65567265, 0x78657472, 0x00000000, 0x00060006, 0x00000019, 0x00000000,
        0x505f6c67, 0x7469736f, 0x006e6f69, 0x00030005, 0x0000001b, 0x00000000, 0x00040005, 0x0000001c,
        0x736f5061, 0x00000000, 0x00060005, 0x0000001e, 0x73755075, 0x6e6f4368, 0x6e617473, 0x00000074,
        0x00050006, 0x0000001e, 0x00000000, 0x61635375, 0x0000656c, 0x00060006, 0x0000001e, 0x00000001,
        0x61725475, 0x616c736e, 0x00006574, 0x00030005, 0x00000020, 0x00006370, 0x00040047, 0x0000000b,
        0x0000001e, 0x00000000, 0x00040047, 0x0000000f, 0x0000001e, 0x00000002, 0x00040047, 0x00000015,
        0x0000001e, 0x00000001, 0x00050048, 0x00000019, 0x00000000, 0x0000000b, 0x00000000, 0x00030047,
        0x00000019, 0x00000002, 0x00040047, 0x0000001c, 0x0000001e, 0x00000000, 0x00050048, 0x0000001e,
        0x00000000, 0x00000023, 0x00000000, 0x00050048, 0x0000001e, 0x00000001, 0x00000023, 0x00000008,
        0x00030047, 0x0000001e, 0x00000002, 0x00020013, 0x00000002, 0x00030021, 0x00000003, 0x00000002,
        0x00030016, 0x00000006, 0x00000020, 0x00040017, 0x00000007, 0x00000006, 0x00000004, 0x00040017,
        0x00000008, 0x00000006, 0x00000002, 0x0004001e, 0x00000009, 0x00000007, 0x00000008, 0x00040020,
        0x0000000a, 0x00000003, 0x00000009, 0x0004003b, 0x0000000a, 0x0000000b, 0x00000003, 0x00040015,
        0x0000000c, 0x00000020, 0x00000001, 0x0004002b, 0x0000000c, 0x0000000d, 0x00000000, 0x00040020,
        0x0000000e, 0x00000001, 0x00000007, 0x0004003b, 0x0000000e, 0x0000000f, 0x00000001, 0x00040020,
        0x00000011, 0x00000003, 0x00000007, 0x0004002b, 0x0000000c, 0x00000013, 0x00000001, 0x00040020,
        0x00000014, 0x00000001, 0x00000008, 0x0004003b, 0x00000014, 0x00000015, 0x00000001, 0x00040020,
        0x00000017, 0x00000003, 0x00000008, 0x0003001e, 0x00000019, 0x00000007, 0x00040020, 0x0000001a,
        0x00000003, 0x00000019, 0x0004003b, 0x0000001a, 0x0000001b, 0x00000003, 0x0004003b, 0x00000014,
        0x0000001c, 0x00000001, 0x0004001e, 0x0000001e, 0x00000008, 0x00000008, 0x00040020, 0x0000001f,
        0x00000009, 0x0000001e, 0x0004003b, 0x0000001f, 0x00000020, 0x00000009, 0x00040020, 0x00000021,
        0x00000009, 0x00000008, 0x0004002b, 0x00000006, 0x00000028, 0x00000000, 0x0004002b, 0x00000006,
        0x00000029, 0x3f800000, 0x00050036, 0x00000002, 0x00000004, 0x00000000, 0x00000003, 0x000200f8,
        0x00000005, 0x0004003d, 0x00000007, 0x00000010, 0x0000000f, 0x00050041, 0x00000011, 0x00000012,
        0x0000000b, 0x0000000d, 0x0003003e, 0x00000012, 0x00000010, 0x0004003d, 0x00000008, 0x00000016,
        0x00000015, 0x00050041, 0x00000017, 0x00000018, 0x0000000b, 0x00000013, 0x0003003e, 0x00000018,
        0x00000016, 0x0004003d, 0x00000008, 0x0000001d, 0x0000001c, 0x00050041, 0x00000021, 0x00000022,
        0x00000020, 0x0000000d, 0x0004003d, 0x00000008, 0x00000023, 0x00000022, 0x00050085, 0x00000008,
        0x00000024, 0x0000001d, 0x00000023, 0x00050041, 0x00000021, 0x00000025, 0x00000020, 0x00000013,
        0x0004003d, 0x00000008, 0x00000026, 0x00000025, 0x00050081, 0x00000008, 0x00000027, 0x00000024,
        0x00000026, 0x00050051, 0x00000006, 0x0000002a, 0x00000027, 0x00000000, 0x00050051, 0x00000006,
        0x0000002b, 0x00000027, 0x00000001, 0x00070050, 0x00000007, 0x0000002c, 0x0000002a, 0x0000002b,
        0x00000028, 0x00000029, 0x00050041, 0x00000011, 0x0000002d, 0x0000001b, 0x0000000d, 0x0003003e,
        0x0000002d, 0x0000002c, 0x000100fd, 0x00010038).toBuffer()

/** glsl_shader.frag, compiled with:
 *  # glslangValidator -V -x -o glsl_shader.frag.u32 glsl_shader.frag
 */
val glslShaderFragSpv = intArrayOf(
        0x07230203, 0x00010000, 0x00080001, 0x0000001e, 0x00000000, 0x00020011, 0x00000001, 0x0006000b,
        0x00000001, 0x4c534c47, 0x6474732e, 0x3035342e, 0x00000000, 0x0003000e, 0x00000000, 0x00000001,
        0x0007000f, 0x00000004, 0x00000004, 0x6e69616d, 0x00000000, 0x00000009, 0x0000000d, 0x00030010,
        0x00000004, 0x00000007, 0x00030003, 0x00000002, 0x000001c2, 0x00040005, 0x00000004, 0x6e69616d,
        0x00000000, 0x00040005, 0x00000009, 0x6c6f4366, 0x0000726f, 0x00030005, 0x0000000b, 0x00000000,
        0x00050006, 0x0000000b, 0x00000000, 0x6f6c6f43, 0x00000072, 0x00040006, 0x0000000b, 0x00000001,
        0x00005655, 0x00030005, 0x0000000d, 0x00006e49, 0x00050005, 0x00000016, 0x78655473, 0x65727574,
        0x00000000, 0x00040047, 0x00000009, 0x0000001e, 0x00000000, 0x00040047, 0x0000000d, 0x0000001e,
        0x00000000, 0x00040047, 0x00000016, 0x00000022, 0x00000000, 0x00040047, 0x00000016, 0x00000021,
        0x00000000, 0x00020013, 0x00000002, 0x00030021, 0x00000003, 0x00000002, 0x00030016, 0x00000006,
        0x00000020, 0x00040017, 0x00000007, 0x00000006, 0x00000004, 0x00040020, 0x00000008, 0x00000003,
        0x00000007, 0x0004003b, 0x00000008, 0x00000009, 0x00000003, 0x00040017, 0x0000000a, 0x00000006,
        0x00000002, 0x0004001e, 0x0000000b, 0x00000007, 0x0000000a, 0x00040020, 0x0000000c, 0x00000001,
        0x0000000b, 0x0004003b, 0x0000000c, 0x0000000d, 0x00000001, 0x00040015, 0x0000000e, 0x00000020,
        0x00000001, 0x0004002b, 0x0000000e, 0x0000000f, 0x00000000, 0x00040020, 0x00000010, 0x00000001,
        0x00000007, 0x00090019, 0x00000013, 0x00000006, 0x00000001, 0x00000000, 0x00000000, 0x00000000,
        0x00000001, 0x00000000, 0x0003001b, 0x00000014, 0x00000013, 0x00040020, 0x00000015, 0x00000000,
        0x00000014, 0x0004003b, 0x00000015, 0x00000016, 0x00000000, 0x0004002b, 0x0000000e, 0x00000018,
        0x00000001, 0x00040020, 0x00000019, 0x00000001, 0x0000000a, 0x00050036, 0x00000002, 0x00000004,
        0x00000000, 0x00000003, 0x000200f8, 0x00000005, 0x00050041, 0x00000010, 0x00000011, 0x0000000d,
        0x0000000f, 0x0004003d, 0x00000007, 0x00000012, 0x00000011, 0x0004003d, 0x00000014, 0x00000017,
        0x00000016, 0x00050041, 0x00000019, 0x0000001a, 0x0000000d, 0x00000018, 0x0004003d, 0x0000000a,
        0x0000001b, 0x0000001a, 0x00050057, 0x00000007, 0x0000001c, 0x00000017, 0x0000001b, 0x00050085,
        0x00000007, 0x0000001d, 0x00000012, 0x0000001c, 0x0003003e, 0x00000009, 0x0000001d, 0x000100fd,
        0x00010038).toBuffer()

const val IMGUI_VK_QUEUED_FRAMES = 2

val PIPELINE_CREATE_FLAGS = 0

data class ImGuiVulkanInitInfo( //TODO: Also raw input for people not using vkk
        val instance: VkInstance,
        val physicalDevice: VkPhysicalDevice,
        val device: VkDevice,
        val queueFamily: Int,
        val queue: VkQueue,
        val pipelineCache: VkPipelineCache,
        val descriptorPool: VkDescriptorPool,
        val allocator: VkAllocationCallbacks?,
        val checkVkResultFn: ((VkResult) -> Nothing)?,
        val commandBuffer: VkCommandBuffer
)

private data class FrameData(
        val backbufferIndex: Int,
        var commandPool: VkCommandPool,
        var commandBuffer: VkCommandBuffer,
        var fence: VkFence,
        var imageAcquiredSemaphore: VkSemaphore,
        var renderCompleteSemaphore: VkSemaphore
)

private data class WindowData(
        var width: Int,
        var height: Int,
        var swapchain: VkSwapchainKHR,
        val surface: VkSurfaceKHR,
        val surfaceFormat: VkSurfaceFormatKHR,
        val presentMode: VkPresentModeKHR,
        val renderPass: VkRenderPass,
        var clearEnable: Boolean,
        var clearValue: VkClearValue,
        var backbufferCount: Int,
        var backBuffer: Array<VkImage>,
        val backBufferView: Array<VkImageView>,
        val framebuffer: Array<VkFramebuffer>,
        var frameIndex: Int,
        val frames: Array<FrameData>
)

private data class FrameDataForRender(
        var vertexBufferMemory: VkDeviceMemory,
        var indexBufferMemory: VkDeviceMemory,
        var vertexBufferSize: VkDeviceSize,
        var indexBufferSize: VkDeviceSize,
        var vertexBuffer: VkBuffer,
        var indexBuffer: VkBuffer
)

class ImplVk(val initInfo: ImGuiVulkanInitInfo, val vkRenderPass: VkRenderPass) : LwjglRendererI {

    val instance = initInfo.instance
    val physicalDevice = initInfo.physicalDevice
    val device = initInfo.device
    val queueFamily = initInfo.queueFamily
    val queue = initInfo.queue
    val pipelineCache = initInfo.pipelineCache
    val descriptorPool = initInfo.descriptorPool
    val allocator = initInfo.allocator
    val checkVkResultFn = initInfo.checkVkResultFn
    val commandBuffer = initInfo.commandBuffer

    var descriptorSetLayout = VkDescriptorSetLayout.NULL
    var pipelineLayout = VkPipelineLayout.NULL
    var descriptorSet = VkDescriptorSet.NULL
    var pipeline = VkPipeline.NULL

    var frameIndex = 0
    private lateinit var framesDataBuffers: Array<FrameDataForRender>

    var fontSampler = VkSampler.NULL
    var fontMemory = VkDeviceMemory.NULL
    var fontImage = VkImage.NULL
    var fontView = VkImageView.NULL
    var uploadBufferMemory = VkDeviceMemory.NULL
    var uploadBuffer = VkBuffer.NULL

    var bufferMemoryAlignment = VkDeviceSize(256)

    private fun checkVkResult(err: VkResult) = checkVkResultFn?.invoke(err)

    override fun createDeviceObjects(): Boolean {
        io.backendRendererName = "imgui impl vulkan"

        var err: VkResult
        val vertInfo: VkShaderModule
        val fragInfo: VkShaderModule

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val sInfos = VkShaderModuleCreateInfo.callocStack(2, stack)
            sInfos.forEach { it.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO) }
            sInfos[0].code = glslShaderVertSpv
            sInfos[1].code = glslShaderFragSpv
            err = vkCreateShaderModule(device, sInfos[0], allocator, lb).vkr
            checkVkResult(err)
            vertInfo = VkShaderModule(lb[0])
            err = vkCreateShaderModule(device, sInfos[1], allocator, lb).vkr
            checkVkResult(err)
            fragInfo = VkShaderModule(lb[0])
        }

        if (fontSampler.isInvalid) {
            stackPush().let { stack ->
                val lb = stack.mallocLong(1)
                val info = vk.SamplerCreateInfo {
                    magFilter = VkFilter.LINEAR
                    minFilter = VkFilter.LINEAR
                    mipmapMode = VkSamplerMipmapMode.LINEAR
                    addressModeU = VkSamplerAddressMode.REPEAT
                    addressModeV = VkSamplerAddressMode.REPEAT
                    addressModeW = VkSamplerAddressMode.REPEAT
                    minLod = -1000.0f
                    maxLod = 1000.0f
                    maxLod = 1.0f
                }
                err = vkCreateSampler(device, info, allocator, lb).vkr
                checkVkResult(err)
                fontSampler = VkSampler(lb[0])
            }
        }

        if (descriptorSetLayout.isInvalid) {
            stackPush().let { stack ->
                val lb = stack.mallocLong(1)
                val binding = vk.DescriptorSetLayoutBinding {
                    descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER
                    descriptorCount = 1
                    stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT
                    immutableSampler = fontSampler
                }
                val createInfo = vk.DescriptorSetLayoutCreateInfo {
                    this.binding = binding
                }
                err = vkCreateDescriptorSetLayout(device, createInfo, allocator, lb).vkr
                checkVkResult(err)
                descriptorSetLayout = VkDescriptorSetLayout(lb[0])
            }
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val allocInfo = vk.DescriptorSetAllocateInfo {
                descriptorPool = descriptorPool
                setLayout = descriptorSetLayout
            }
            err = vkAllocateDescriptorSets(device, allocInfo, lb).vkr
            checkVkResult(err)
            descriptorSet = VkDescriptorSet(lb[0])
        }

        if (pipelineLayout.isInvalid) {
            stackPush().let { stack ->
                val lb = stack.mallocLong(1)
                val pushConstants = vk.PushConstantRange {
                    stageFlags = VK_SHADER_STAGE_VERTEX_BIT
                    offset = Float.BYTES * 0
                    size = Float.BYTES * 4
                }
                val layoutInfo = vk.PipelineLayoutCreateInfo {
                    setLayout = descriptorSetLayout
                    pushConstantRange = pushConstants
                }
                err = vkCreatePipelineLayout(device, layoutInfo, allocator, lb).vkr
                checkVkResult(err)
                pipelineLayout = VkPipelineLayout(lb[0])
            }
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val stage = VkPipelineShaderStageCreateInfo.callocStack(2, stack)
            stage[0].sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            stage[0].stage = VkShaderStage.VERTEX_BIT
            stage[0].module = vertInfo
            stage[0].pName = stack.ASCII("main")
            stage[1].sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            stage[1].stage = VkShaderStage.FRAGMENT_BIT
            stage[1].module = fragInfo
            stage[1].pName = stack.ASCII("main")

            val bindingDesc = vk.VertexInputBindingDescription {
                stride = DrawVert.size
                inputRate = VkVertexInputRate.VERTEX
            }

            val attributeDesc = VkVertexInputAttributeDescription.callocStack(2, stack)
            attributeDesc[0].location = 0
            attributeDesc[0].binding = bindingDesc.binding
            attributeDesc[0].format = VkFormat.R32G32_SFLOAT //VK_FORMAT_R32G32_SFLOAT
            attributeDesc[0].offset = DrawVert.ofsPos
            attributeDesc[1].location = 1
            attributeDesc[1].binding = bindingDesc.binding
            attributeDesc[1].format = VkFormat.R32G32_SFLOAT //VK_FORMAT_R32G32_SFLOAT
            attributeDesc[1].offset = DrawVert.ofsUv
            attributeDesc[2].location = 2
            attributeDesc[2].binding = bindingDesc.binding
            attributeDesc[2].format = VkFormat.R8G8B8A8_UNORM //VK_FORMAT_R8G8B8A8_UNORM
            attributeDesc[2].offset = DrawVert.ofsCol

            val vertexInfo = vk.PipelineVertexInputStateCreateInfo {
                vertexBindingDescription = bindingDesc
                vertexAttributeDescriptions = attributeDesc
            }

            val iaInfo = vk.PipelineInputAssemblyStateCreateInfo {
                topology = VkPrimitiveTopology.TRIANGLE_LIST
            }

            val viewportInfo = vk.PipelineViewportStateCreateInfo {
                viewportCount = 1
                scissorCount = 1
            }

            val rasterInfo = vk.PipelineRasterizationStateCreateInfo {
                polygonMode = VkPolygonMode.FILL
                cullMode = VkCullMode.NONE.i
                frontFace = VkFrontFace.COUNTER_CLOCKWISE
                lineWidth = 1.0f
            }

            val msInfo = vk.PipelineMultisampleStateCreateInfo {
                rasterizationSamples = VkSampleCount._1_BIT
            }

            val colorAttachment = vk.PipelineColorBlendAttachmentState {
                blendEnable = true
                srcColorBlendFactor = VkBlendFactor.SRC_ALPHA
                dstColorBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
                colorBlendOp = VkBlendOp.ADD
                srcAlphaBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
                dstAlphaBlendFactor = VkBlendFactor.ZERO
                alphaBlendOp = VkBlendOp.ADD
                colorWriteMask = VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT
            }

            val depthInfo = vk.PipelineDepthStencilStateCreateInfo {}

            val blendInfo = vk.PipelineColorBlendStateCreateInfo {
                attachment = colorAttachment
            }

            val dynStates = VkDynamicStateBuffer(listOf(VkDynamicState.VIEWPORT, VkDynamicState.SCISSOR))

            val dynamicState = vk.PipelineDynamicStateCreateInfo {
                dynamicStates = dynStates
            }

            val info = vk.GraphicsPipelineCreateInfo {
                flags = PIPELINE_CREATE_FLAGS
                stages = stage
                vertexInputState = vertexInfo
                inputAssemblyState = iaInfo
                viewportState = viewportInfo
                rasterizationState = rasterInfo
                multisampleState = msInfo
                depthStencilState = depthInfo
                colorBlendState = blendInfo
                this.dynamicState = dynamicState
                layout = pipelineLayout
                this.renderPass = vkRenderPass
            }

            err = vkCreateGraphicsPipelines(device, pipelineCache, VkGraphicsPipelineCreateInfo.callocStack(1).put(0, info), allocator, lb).vkr
            checkVkResult(err)

            pipeline = VkPipeline(lb[0])

            vkDestroyShaderModule(device, vertInfo, allocator)
            vkDestroyShaderModule(device, fragInfo, allocator)
        }

        createFontsTexture()

        return true
    }

    private fun createFontsTexture(): Boolean {
        if (io.fonts.isBuilt)
            return true

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, fontImageSize) = io.fonts.getTexDataAsRGBA32()

        val uploadSize = VkDeviceSize(fontImageSize.x * fontImageSize.y * 4 * Byte.BYTES.L)

        var err: VkResult

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val info = vk.ImageCreateInfo {
                imageType = VkImageType._2D
                format = VkFormat.R8G8B8A8_UNORM
                extent.width = fontImageSize.x
                extent.height = fontImageSize.y
                extent.depth = 1
                mipLevels = 1
                arrayLayers = 1
                samples = VkSampleCount._1_BIT
                tiling = VkImageTiling.OPTIMAL
                usage = VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT
                sharingMode = VkSharingMode.EXCLUSIVE
                initialLayout = VkImageLayout.UNDEFINED
            }
            err = vkCreateImage(device, info, allocator, lb).vkr
            checkVkResult(err)
            fontImage = VkImage(lb[0])

            val req = VkMemoryRequirements.callocStack(stack)
            vkGetImageMemoryRequirements(device, fontImage, req)

            val allocInfo = vk.MemoryAllocateInfo {
                allocationSize = req.size
                memoryTypeIndex = memoryType(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, req.memoryTypeBits)
            }
            err = vkAllocateMemory(device, allocInfo, allocator, lb).vkr
            checkVkResult(err)
            fontMemory = VkDeviceMemory(lb[0])
            err = vkBindImageMemory(device, fontImage, fontMemory, 0).vkr
            checkVkResult(err)
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val info = vk.ImageViewCreateInfo {
                image = fontImage
                viewType = VkImageViewType._2D
                format = VkFormat.R8G8B8A8_UNORM
                subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
                subresourceRange.levelCount = 1
                subresourceRange.layerCount = 1
            }
            err = vkCreateImageView(device, info, allocator, lb).vkr
            checkVkResult(err)
            fontView = VkImageView(lb[0])
        }

        stackPush().let { stack ->
            val descImage = vk.DescriptorImageInfo {
                sampler = fontSampler
                imageView = fontView
                imageLayout = VkImageLayout.SHADER_READ_ONLY_OPTIMAL
            }

            val writeDesc = vk.WriteDescriptorSet {
                dstSet = descriptorSet
                descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER
                imageInfo = descImage
            }

            vkUpdateDescriptorSets(device, VkWriteDescriptorSet.callocStack(1).put(0, writeDesc), null)
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val bufferInfo = vk.BufferCreateInfo {
                size = uploadSize
                usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT
                sharingMode = VkSharingMode.EXCLUSIVE
            }
            err = vkCreateBuffer(device, bufferInfo, allocator, lb).vkr
            checkVkResult(err)
            uploadBuffer = VkBuffer(lb[0])

            val req = VkMemoryRequirements.callocStack(stack)
            vkGetBufferMemoryRequirements(device, uploadBuffer, req)

            bufferMemoryAlignment = if (bufferMemoryAlignment.L > req.alignment.L) bufferMemoryAlignment else req.alignment

            val allocInfo = vk.MemoryAllocateInfo {
                allocationSize = req.size
                memoryTypeIndex = memoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, req.memoryTypeBits)
            }
            err = vkAllocateMemory(device, allocInfo, allocator, lb).vkr
            checkVkResult(err)
            uploadBufferMemory = VkDeviceMemory(lb[0])
            err = vkBindBufferMemory(device, uploadBuffer, uploadBufferMemory, 0).vkr
            checkVkResult(err)
        }

        stackPush().let { stack ->
            val pb = stack.mallocPointer(1)
            err = vkMapMemory(device, uploadBufferMemory, 0, uploadSize, 0, pb).vkr
            checkVkResult(err)
            pixels.copyTo(pb[0])
            val range = vk.MappedMemoryRange {
                memory = uploadBufferMemory
                size = uploadSize
            }
            err = vkFlushMappedMemoryRanges(device, range).vkr
            checkVkResult(err)
            vkUnmapMemory(device, pb[0])
        }

        stackPush().let { stack ->
            val copyBarrier = vk.ImageMemoryBarrier {
                dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT
                oldLayout = VkImageLayout.UNDEFINED
                newLayout = VkImageLayout.TRANSFER_DST_OPTIMAL
                srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                image = fontImage
                subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
                subresourceRange.levelCount = 1
                subresourceRange.layerCount = 1
            }
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, VkImageMemoryBarrier.callocStack(1).put(0, copyBarrier))

            val region = vk.BufferImageCopy {
                imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
                imageSubresource.layerCount = 1
                imageExtent.width = fontImageSize.x
                imageExtent.height = fontImageSize.y
                imageExtent.depth = 1
            }
            vkCmdCopyBufferToImage(commandBuffer, uploadBuffer, fontImage, VkImageLayout.TRANSFER_DST_OPTIMAL, VkBufferImageCopy.callocStack(1).put(0, region))

            val useBarrier = vk.ImageMemoryBarrier {
                srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT
                dstAccessMask = VK_ACCESS_SHADER_READ_BIT
                oldLayout = VkImageLayout.TRANSFER_DST_OPTIMAL
                newLayout = VkImageLayout.SHADER_READ_ONLY_OPTIMAL
                srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                image = fontImage
                subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
                subresourceRange.levelCount = 1
                subresourceRange.layerCount = 1
            }
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, VkImageMemoryBarrier.callocStack(1).put(0, useBarrier))
        }

        io.fonts.texId = fontImage.L.i

        return true
    }

    override fun renderDrawData(drawData: DrawData) {
        val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
        val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
        if (fbWidth == 0 || fbHeight == 0) return

        var err: VkResult
        val fd = framesDataBuffers[frameIndex]
        frameIndex = (frameIndex + 1) % IMGUI_VK_QUEUED_FRAMES

        val vertex_size = drawData.totalVtxCount * DrawVert.size
        val index_size = drawData.totalIdxCount * DrawIdx.SIZE_BYTES
        if (fd.vertexBuffer.isInvalid || fd.vertexBufferSize.i < vertex_size) {
            val (newBuffer, newBufferMemory, newDeviceSize) = createOrResizeBuffer(fd.vertexBuffer, fd.vertexBufferMemory, VkDeviceSize(vertex_size.L), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
            fd.vertexBuffer = newBuffer
            fd.vertexBufferMemory = newBufferMemory
            fd.vertexBufferSize = newDeviceSize
        }
        if (fd.indexBuffer.isInvalid || fd.indexBufferSize.i < index_size) {
            val (newBuffer, newBufferMemory, newDeviceSize) = createOrResizeBuffer(fd.indexBuffer, fd.indexBufferMemory, VkDeviceSize(index_size.L), VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
            fd.indexBuffer = newBuffer
            fd.indexBufferMemory = newBufferMemory
            fd.indexBufferSize = newDeviceSize
        }

        stackPush().let { stack ->
            val pb = stack.mallocPointer(1)
            err = vkMapMemory(device, fd.vertexBufferMemory, 0, VkDeviceSize(vertex_size.L), 0, pb).vkr
            checkVkResult(err)
            val vtxDst = MemoryUtil.memByteBuffer(pb[0], vertex_size)
            err = vkMapMemory(device, fd.indexBufferMemory, 0, VkDeviceSize(index_size.L), 0, pb).vkr
            checkVkResult(err)
            val idxDst = MemoryUtil.memByteBuffer(pb[0], index_size)
            var vtxOff = 0
            var idxOff = 0
            for (cmdList in drawData.cmdLists) {
                cmdList.vtxBuffer.forEachIndexed { i, v ->
                    val offset = (vtxOff + i) * DrawVert.size
                    v.pos.to(vtxDst, offset)
                    v.uv.to(vtxDst, offset + DrawVert.ofsUv)
                    vtxDst.putInt(offset + DrawVert.ofsCol, v.col)
                }
                cmdList.idxBuffer.forEachIndexed { i, idx -> idxDst.putInt(idxOff + i, idx) }

                vtxOff += cmdList.vtxBuffer.size
                idxOff += cmdList.idxBuffer.size
            }
            val range = VkMappedMemoryRange.callocStack(2, stack)
            range[0].sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
            range[0].memory = fd.vertexBufferMemory
            range[0].size = VkDeviceSize(0)
            range[1].sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
            range[1].memory = fd.indexBufferMemory
            range[1].size = VkDeviceSize(0)
            err = vkFlushMappedMemoryRanges(device, range).vkr
            checkVkResult(err)
            vkUnmapMemory(device, fd.vertexBufferMemory)
            vkUnmapMemory(device, fd.indexBufferMemory)
        }

        run {
            vkCmdBindPipeline(commandBuffer, VkPipelineBindPoint.GRAPHICS, pipeline)
            vkCmdBindDescriptorSets(commandBuffer, VkPipelineBindPoint.GRAPHICS, pipelineLayout, 0, descriptorSet, null as IntArray?)
        }

        run {
            vkCmdBindVertexBuffers(commandBuffer, 0, fd.vertexBuffer, VkDeviceSize(0))
            vkCmdBindIndexBuffer(commandBuffer, fd.indexBuffer, VkDeviceSize(0L), VkIndexType.UINT16)
        }

        stackPush().let { stack ->
            val viewport = VkViewport.callocStack(1, stack)
            viewport[0].x = 0.0f
            viewport[0].y = 0.0f
            viewport[0].width = fbWidth.f
            viewport[0].height = fbHeight.f
            viewport[0].minDepth = 0.0f
            viewport[0].maxDepth = 1.0f
            vkCmdSetViewport(commandBuffer, 0, viewport)
        }

        run {
            val scale = FloatArray(2)
            scale[0] = 2.0f / drawData.displaySize.x
            scale[1] = 2.0f / drawData.displaySize.y
            val translate = FloatArray(2)
            translate[0] = -1.0f - drawData.displayPos.x * scale[0]
            translate[1] = -1.0f - drawData.displayPos.y * scale[1]
            vkCmdPushConstants(commandBuffer, pipelineLayout, VkShaderStage.VERTEX_BIT.i, Float.BYTES * 0, scale)
            vkCmdPushConstants(commandBuffer, pipelineLayout, VkShaderStage.VERTEX_BIT.i, Float.BYTES * 2, translate)
        }

        val clip_off = drawData.displayPos         // (0,0) unless using multi-viewports
        val clip_scale = drawData.framebufferScale // (1,1) unless using retina display which are often (2,2)

        // Render command lists
        var vtx_offset = 0
        var idx_offset = 0
        for (cmd_list in drawData.cmdLists) {
            for (pcmd in cmd_list.cmdBuffer) {
                if (pcmd.userCallback != null) {
                    pcmd.userCallback!!.invoke(cmd_list, pcmd)
                } else {
                    // Project scissor/clipping rectangles into framebuffer space
                    val clip_rect = Vec4(
                            (pcmd.clipRect.x - clip_off.x) * clip_scale.x,
                            (pcmd.clipRect.y - clip_off.y) * clip_scale.y,
                            (pcmd.clipRect.z - clip_off.x) * clip_scale.x,
                            (pcmd.clipRect.w - clip_off.y) * clip_scale.y
                    )

                    if (clip_rect.x < fbWidth && clip_rect.y < fbHeight && clip_rect.z >= 0.0f && clip_rect.w >= 0.0f) {
                        // Apply scissor/clipping rectangle
                        val scissor = VkRect2D.callocStack(1)
                        scissor[0].offset.x = clip_rect.x.i
                        scissor[0].offset.y = clip_rect.y.i
                        scissor[0].extent.width = (clip_rect.z - clip_rect.x).i
                        scissor[0].extent.height = (clip_rect.w - clip_rect.y).i
                        vkCmdSetScissor(commandBuffer, 0, scissor)

                        // Draw
                        vkCmdDrawIndexed(commandBuffer, pcmd.elemCount, 1, idx_offset, vtx_offset, 0)
                    }
                }
                idx_offset += pcmd.elemCount
            }
            vtx_offset += cmd_list.vtxBuffer.size
        }
    }

    private fun destroyFontObjects() {
        if (uploadBuffer.isValid) {
            vkDestroyBuffer(device, uploadBuffer, allocator)
            uploadBuffer = VkBuffer.NULL
        }
        if (uploadBufferMemory.isValid) {
            vkFreeMemory(device, uploadBufferMemory, allocator)
            uploadBufferMemory = VkDeviceMemory.NULL
        }
    }

    override fun destroyDeviceObjects() {
        destroyFontObjects()

        for (fd in framesDataBuffers) {
            if (fd.vertexBuffer.isValid) {
                vkDestroyBuffer(device, fd.vertexBuffer, allocator)
                fd.vertexBuffer = VkBuffer.NULL
            }
            if (fd.vertexBufferMemory.isValid) {
                vkFreeMemory(device, fd.vertexBufferMemory, allocator)
                fd.vertexBufferMemory = VkDeviceMemory.NULL
            }

            if (fd.indexBuffer.isValid) {
                vkDestroyBuffer(device, fd.indexBuffer, allocator)
                fd.indexBuffer = VkBuffer.NULL
            }
            if (fd.indexBufferMemory.isValid) {
                vkFreeMemory(device, fd.indexBufferMemory, allocator)
                fd.indexBufferMemory = VkDeviceMemory.NULL
            }
        }

        if (fontView.isValid) {
            vkDestroyImageView(device, fontView, allocator)
            fontView = VkImageView.NULL
        }

        if (fontImage.isValid) {
            vkDestroyImage(device, fontImage, allocator)
            fontImage = VkImage.NULL
        }

        if (fontMemory.isValid) {
            vkFreeMemory(device, fontMemory, allocator)
            fontMemory = VkDeviceMemory.NULL
        }

        if (fontSampler.isValid) {
            vkDestroySampler(device, fontSampler, allocator)
            fontSampler = VkSampler.NULL
        }

        if (descriptorSetLayout.isValid) {
            vkDestroyDescriptorSetLayout(device, descriptorSetLayout, allocator)
            descriptorSetLayout = VkDescriptorSetLayout.NULL
        }

        if (pipelineLayout.isValid) {
            vkDestroyPipelineLayout(device, pipelineLayout, allocator)
            pipelineLayout = VkPipelineLayout.NULL
        }

        if (pipeline.isValid) {
            vkDestroyPipeline(device, pipeline, allocator)
            pipeline = VkPipeline.NULL
        }
    }

    fun memoryType(properties: VkMemoryPropertyFlags, typeBits: Int): Int {
        for (i in 0 until physicalDevice.memoryProperties.memoryTypeCount()) {
            if ((physicalDevice.memoryProperties.memoryTypes(i).propertyFlags() and properties) == properties && (typeBits and (1 shl i)) != 0)
                return i
        }
        return -1
    }

    fun createOrResizeBuffer(buffer: VkBuffer, bufferMemory: VkDeviceMemory, newSize: VkDeviceSize, usage: VkBufferUsageFlags): Triple<VkBuffer, VkDeviceMemory, VkDeviceSize> {
        var err: VkResult

        if (buffer.isValid)
            vkDestroyBuffer(device, buffer, allocator)
        if (bufferMemory.isValid)
            vkFreeMemory(device, bufferMemory, allocator)

        val retBuffer: VkBuffer
        val retMem: VkDeviceMemory

        val vertexBufferSizeAligned = VkDeviceSize(((newSize.L - 1) / bufferMemoryAlignment.L + 1) * bufferMemoryAlignment.L)
        stackPush().let { stack ->
            val bufferInfo = vk.BufferCreateInfo {
                size = vertexBufferSizeAligned
                this.usage = usage
                sharingMode = VkSharingMode.EXCLUSIVE
            }
            val lb = stack.mallocLong(1)
            err = vkCreateBuffer(device, bufferInfo, allocator, lb).vkr
            checkVkResult(err)
            retBuffer = VkBuffer(lb[0])

            val req = VkMemoryRequirements.mallocStack(stack)
            vkGetBufferMemoryRequirements(device, retBuffer, req)
            bufferMemoryAlignment = if (bufferMemoryAlignment.L > req.alignment.L) bufferMemoryAlignment else req.alignment
            val allocInfo = vk.MemoryAllocateInfo {
                allocationSize = VkDeviceSize(req.size())
                memoryTypeIndex = memoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, req.memoryTypeBits)
            }
            err = vkAllocateMemory(device, allocInfo, allocator, lb).vkr
            checkVkResult(err)
            retMem = VkDeviceMemory(lb[0])
        }

        err = vkBindBufferMemory(device, retBuffer, retMem, 0).vkr
        checkVkResult(err)

        return Triple(retBuffer, retMem, newSize)
    }

    fun getMinImageCountFromPresentMode(presentMode: VkPresentModeKHR): Int {
        if (presentMode == VkPresentModeKHR.MAILBOX_KHR)
            return 3
        if (presentMode == VkPresentModeKHR.FIFO_KHR || presentMode == VkPresentModeKHR.FIFO_RELAXED_KHR)
            return 2
        if (presentMode == VkPresentModeKHR.IMMEDIATE_KHR)
            return 1
        assert(false)
        return 1
    }

    fun selectSurfaceFormat(physicalDevice: VkPhysicalDevice, surface: VkSurfaceKHR, requestFormats: Array<VkFormat>, requestColorSpace: VkColorSpaceKHR): VkSurfaceFormatKHR {
        assert(requestFormats.isNotEmpty())

        stackPush().let { stack ->
            val ib = stack.mallocInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.L, ib, null)
            val surfaceFormats = VkSurfaceFormatKHR.callocStack(ib[0], stack)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.L, ib, surfaceFormats)

            if (ib[0] == 1) {
                return if (surfaceFormats[0].format() == VK_FORMAT_UNDEFINED) {
                    val ret = VkSurfaceFormatKHR.calloc()
                    ret.format = requestFormats[0]
                    ret.colorSpace = requestColorSpace
                    ret
                } else {
                    surfaceFormats[0]
                }
            } else {
                for (request_i in 0 until requestFormats.size)
                    for (avail_i in 0 until ib[0])
                        if (surfaceFormats[avail_i].format == requestFormats[request_i] && surfaceFormats[avail_i].colorSpace == requestColorSpace)
                            return surfaceFormats[avail_i]
                return surfaceFormats[0]
            }
        }
    }

    fun selectPresentMode(physicalDevice: VkPhysicalDevice, surface: VkSurfaceKHR, requestModes: Array<VkPresentModeKHR>): VkPresentModeKHR {
        assert(requestModes.isNotEmpty())

        stackPush().let { stack ->
            val ib = stack.mallocInt(1)
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.L, ib, null)
            val availModes = stack.mallocInt(ib[0])
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.L, ib, availModes)

            for (request_i in 0 until requestModes.size)
                for (avail_i in 0 until ib[0])
                    if (requestModes[request_i].i == availModes[avail_i])
                        return requestModes[request_i]

            return VkPresentModeKHR.FIFO_KHR
        }
    }

    private fun createWindowDataCommandBuffers(physicalDevice: VkPhysicalDevice, device: VkDevice, queueFamily: Int, wd: WindowData, allocator: VkAllocationCallbacks) {
        var err: VkResult

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val pb = stack.mallocPointer(1)
            for (i in 0 until IMGUI_VK_QUEUED_FRAMES) {
                val fd = wd.frames[i]

                run {
                    val info = vk.CommandPoolCreateInfo {
                        flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
                        queueFamilyIndex = queueFamily
                    }
                    err = vkCreateCommandPool(device, info, allocator, lb).vkr
                    checkVkResult(err)
                    fd.commandPool = VkCommandPool(lb[0])
                }

                run {
                    val info = vk.CommandBufferAllocateInfo {
                        commandPool = fd.commandPool
                        level = VkCommandBufferLevel.PRIMARY
                        commandBufferCount = 1
                    }
                    err = vkAllocateCommandBuffers(device, info, pb).vkr
                    checkVkResult(err)
                    fd.commandBuffer = VkCommandBuffer(pb[0], device)
                }

                run {
                    val info = vk.FenceCreateInfo(VkFenceCreate.SIGNALED_BIT)
                    err = vkCreateFence(device, info, allocator, lb).vkr
                    checkVkResult(err)
                    fd.fence = VkFence(lb[0])
                }

                run {
                    val info = vk.SemaphoreCreateInfo{}
                    err = vkCreateSemaphore(device, info, allocator, lb).vkr
                    checkVkResult(err)
                    fd.imageAcquiredSemaphore = VkSemaphore(lb[0])
                    err = vkCreateSemaphore(device, info, allocator, lb).vkr
                    checkVkResult(err)
                    fd.renderCompleteSemaphore = VkSemaphore(lb[0])
                }
            }
        }
    }

    private fun destroyWindowData(instance: VkInstance, device: VkDevice, wd: WindowData, allocator: VkAllocationCallbacks) {
        vkDeviceWaitIdle(device)

        for (i in 0 until IMGUI_VK_QUEUED_FRAMES) {
            val fd = wd.frames[0]
            vkDestroyFence(device, fd.fence, allocator)
            vkFreeCommandBuffers(device, fd.commandPool, fd.commandBuffer)
            vkDestroyCommandPool(device, fd.commandPool, allocator)
            vkDestroySemaphore(device, fd.imageAcquiredSemaphore, allocator)
            vkDestroySemaphore(device, fd.renderCompleteSemaphore, allocator)
        }

        for (i in 0 until wd.backbufferCount) {
            vkDestroyImageView(device, wd.backBufferView[i], allocator)
            vkDestroyFramebuffer(device, wd.framebuffer[i], allocator)
        }

        vkDestroyRenderPass(device, wd.renderPass, allocator)
        vkDestroySwapchainKHR(device, wd.swapchain.L, allocator)
        vkDestroySurfaceKHR(instance, wd.surface.L, allocator)

    }

    private fun createWindowDataSwapChainAndFramebuffer(physicalDevice: VkPhysicalDevice, device: VkDevice, wd: WindowData, allocator: VkAllocationCallbacks, w: Int, h: Int) {
        var minImageCount = 2

        val oldSwapchain = wd.swapchain
        var err = device.waitIdle()
        checkVkResult(err)

        for (i in 0 until wd.backbufferCount) {
            if (wd.backBufferView[i].isValid)
                vkDestroyImageView(device, wd.backBufferView[i], allocator)
            if (wd.framebuffer[i].isValid)
                vkDestroyFramebuffer(device, wd.framebuffer[i], allocator)
        }
        wd.backbufferCount = 0
        if (wd.renderPass.isValid)
            vkDestroyRenderPass(device, wd.renderPass, allocator)

        if (minImageCount == 0)
            minImageCount = getMinImageCountFromPresentMode(wd.presentMode)

        stackPush().let { stack ->
            val info = vk.SwapchainCreateInfoKHR {
                surface = wd.surface
                this.minImageCount = minImageCount
                imageFormat = wd.surfaceFormat.format
                imageColorSpace = wd.surfaceFormat.colorSpace
                imageArrayLayers = 1
                imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
                imageSharingMode = VkSharingMode.EXCLUSIVE           // Assume that graphics family == present family
                preTransform = VkSurfaceTransformKHR.IDENTITY_BIT_KHR
                compositeAlpha = VkCompositeAlphaKHR.OPAQUE_BIT_KHR
                presentMode = wd.presentMode
                clipped = true
                this.oldSwapchain = oldSwapchain
            }

            val cap = vk.SurfaceCapabilitiesKHR{}
            err = physicalDevice.getSurfaceCapabilitiesKHR(wd.surface, cap)
            checkVkResult(err)
            if (info.minImageCount < cap.minImageCount)
                info.minImageCount = cap.minImageCount
            else if (cap.maxImageCount != 0 && info.minImageCount > cap.maxImageCount)
                info.minImageCount = cap.maxImageCount

            if (cap.currentExtent.width == (0xffffffff).toInt()) {
                info.imageExtent.width = w
                wd.width = w
                info.imageExtent.height = w
                wd.height = h
            } else {
                info.imageExtent.width = cap.currentExtent.width
                wd.width = cap.currentExtent.width
                info.imageExtent.height = cap.currentExtent.height
                wd.height = cap.currentExtent.height
            }

            val lb = stack.mallocLong(1)
            val ib = stack.mallocInt(1)
            err = vkCreateSwapchainKHR(device, info, allocator, lb).vkr
            checkVkResult(err)
            wd.swapchain = VkSwapchainKHR(lb[0])
            err = vkGetSwapchainImagesKHR(device, wd.swapchain.L, ib, null).vkr
            checkVkResult(err)
            val imgs = stack.mallocLong(ib[0])
            err = vkGetSwapchainImagesKHR(device, wd.swapchain.L, ib, imgs).vkr
            checkVkResult(err)
            wd.backBuffer = Array(ib[0]) { VkImage(imgs[it]) }

            if (oldSwapchain.isValid)
                vkDestroySwapchainKHR(device, oldSwapchain.L, allocator)
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val attachment = vk.AttachmentDescription {
                format = wd.surfaceFormat.format
                samples = VkSampleCount._1_BIT
                loadOp = if (wd.clearEnable) VkAttachmentLoadOp.CLEAR else VkAttachmentLoadOp.DONT_CARE
                storeOp = VkAttachmentStoreOp.STORE
                stencilLoadOp = VkAttachmentLoadOp.DONT_CARE
                stencilStoreOp = VkAttachmentStoreOp.DONT_CARE
                initialLayout = VkImageLayout.UNDEFINED
                finalLayout = VkImageLayout.PRESENT_SRC_KHR
            }
            val colorAttachment = vk.AttachmentReference {
                this.attachment = 0
                layout = VkImageLayout.COLOR_ATTACHMENT_OPTIMAL
            }
            val subpass = vk.SubpassDescription {
                pipelineBindPoint = VkPipelineBindPoint.GRAPHICS
                colorAttachmentCount = 1
                this.colorAttachment = colorAttachment
            }
            val dependency = vk.SubpassDependency {
                srcSubpass = VK_SUBPASS_EXTERNAL
                dstSubpass = 0
                srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                srcAccessMask = 0
                dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
            }
            val info = vk.RenderPassCreateInfo {
                this.attachment = attachment
                this.subpass = subpass
                this.dependency = dependency
            }
            err = vkCreateRenderPass(device, info, allocator, lb).vkr
            checkVkResult(err)
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val info = vk.ImageViewCreateInfo {
                viewType = VkImageViewType._2D
                format = wd.surfaceFormat.format
                components.r = VkComponentSwizzle.R
                components.g = VkComponentSwizzle.G
                components.b = VkComponentSwizzle.B
                components.a = VkComponentSwizzle.A
                subresourceRange = vk.ImageSubresourceRange(VkImageAspect.COLOR_BIT.i, 0, 1, 0, 1)
            }
            for (i in 0 until wd.backbufferCount) {
                info.image = wd.backBuffer[i]
                err = vkCreateImageView(device, info, allocator, lb).vkr
                checkVkResult(err)
                wd.backBufferView[i] = VkImageView(lb[0])
            }
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            var attachment = VkImageView.NULL
            val info = vk.FramebufferCreateInfo {
                renderPass = wd.renderPass
                this.attachment = attachment
                width = wd.width
                height = wd.height
                layers = 1
            }
            for (i in 0 until wd.backbufferCount) {
                attachment = wd.backBufferView[i]
                err = vkCreateFramebuffer(device, info, allocator, lb).vkr
                checkVkResult(err)
                wd.framebuffer[i] = VkFramebuffer(lb[0])
            }
        }
    }
}

private val Int.vkr: VkResult
    get() = VkResult(this)
