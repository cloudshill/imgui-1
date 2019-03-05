package imgui.impl

import glm_.*
import glm_.vec2.Vec2
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
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT16
import org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer


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

class ImplVk(val initInfo: ImGuiVulkanInitInfo, val renderPass: VkRenderPass) : LwjglRendererI {

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
                val info = VkSamplerCreateInfo.callocStack(stack)
                info.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                info.magFilter(VK_FILTER_LINEAR)
                info.minFilter(VK_FILTER_LINEAR)
                info.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                info.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                info.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                info.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                info.minLod(-1000.0f)
                info.maxLod(1000.0f)
                info.maxLod(1.0f)
                err = vkCreateSampler(device, info, allocator, lb).vkr
                checkVkResult(err)
                fontSampler = VkSampler(lb[0])
            }
        }

        if (descriptorSetLayout.isInvalid) {
            stackPush().let { stack ->
                val lb = stack.mallocLong(1)
                val samplers = stack.longs(fontSampler.L)
                val binding = VkDescriptorSetLayoutBinding.callocStack(1, stack)
                binding[0].descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                binding[0].descriptorCount(1)
                binding[0].stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                binding[0].pImmutableSamplers(samplers)
                val createInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack)
                createInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                createInfo.pBindings(binding)
                err = vkCreateDescriptorSetLayout(device, createInfo, allocator, lb).vkr
                checkVkResult(err)
                descriptorSetLayout = VkDescriptorSetLayout(lb[0])
            }
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack)
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
            allocInfo.descriptorPool(descriptorPool.L)
            allocInfo.pSetLayouts(stack.longs(descriptorSetLayout.L))
            err = vkAllocateDescriptorSets(device, allocInfo, lb).vkr
            checkVkResult(err)
            descriptorSet = VkDescriptorSet(lb[0])
        }

        if (pipelineLayout.isInvalid) {
            stackPush().let { stack ->
                val lb = stack.mallocLong(1)
                val pushConstants = VkPushConstantRange.callocStack(1, stack)
                pushConstants[0].stageFlags = VK_SHADER_STAGE_VERTEX_BIT
                pushConstants[0].offset = Float.BYTES * 0
                pushConstants[0].size = Float.BYTES * 4
                val layoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack)
                layoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                layoutInfo.setLayouts = stack.longs(descriptorSetLayout.L)
                layoutInfo.pushConstantRanges = pushConstants
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

            val bindingDesc = VkVertexInputBindingDescription.callocStack(2, stack)
            bindingDesc[0].stride = DrawVert.size
            bindingDesc[0].inputRate = VkVertexInputRate.VERTEX

            val attributeDesc = VkVertexInputAttributeDescription.callocStack(2, stack)
            attributeDesc[0].location = 0
            attributeDesc[0].binding = bindingDesc[0].binding
            attributeDesc[0].format = VkFormat.R32G32_SFLOAT //VK_FORMAT_R32G32_SFLOAT
            attributeDesc[0].offset = DrawVert.ofsPos
            attributeDesc[1].location = 1
            attributeDesc[1].binding = bindingDesc[0].binding
            attributeDesc[1].format = VkFormat.R32G32_SFLOAT //VK_FORMAT_R32G32_SFLOAT
            attributeDesc[1].offset = DrawVert.ofsUv
            attributeDesc[2].location = 2
            attributeDesc[2].binding = bindingDesc[0].binding
            attributeDesc[2].format = VkFormat.R8G8B8A8_UNORM //VK_FORMAT_R8G8B8A8_UNORM
            attributeDesc[2].offset = DrawVert.ofsCol

            val vertexInfo = VkPipelineVertexInputStateCreateInfo.callocStack(stack)
            vertexInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            vertexInfo.vertexBindingDescriptions = bindingDesc
            vertexInfo.vertexAttributeDescriptions = attributeDesc

            val iaInfo = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack)
            iaInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            iaInfo.topology = VkPrimitiveTopology.TRIANGLE_LIST

            val viewportInfo = VkPipelineViewportStateCreateInfo.callocStack(stack)
            viewportInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            viewportInfo.viewportCount = 1
            viewportInfo.scissorCount = 1

            val rasterInfo = VkPipelineRasterizationStateCreateInfo.callocStack(stack)
            rasterInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            rasterInfo.polygonMode = VkPolygonMode.FILL
            rasterInfo.cullMode = VkCullMode.NONE.i
            rasterInfo.frontFace = VkFrontFace.COUNTER_CLOCKWISE
            rasterInfo.lineWidth = 1.0f

            val msInfo = VkPipelineMultisampleStateCreateInfo.callocStack(stack)
            msInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            msInfo.rasterizationSamples = VkSampleCount._1_BIT

            val colorAttachment = VkPipelineColorBlendAttachmentState.callocStack(1, stack)
            colorAttachment[0].blendEnable = true
            colorAttachment[0].srcColorBlendFactor = VkBlendFactor.SRC_ALPHA
            colorAttachment[0].dstColorBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
            colorAttachment[0].colorBlendOp = VkBlendOp.ADD
            colorAttachment[0].srcAlphaBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
            colorAttachment[0].dstAlphaBlendFactor = VkBlendFactor.ZERO
            colorAttachment[0].alphaBlendOp = VkBlendOp.ADD
            colorAttachment[0].colorWriteMask = VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT

            val depthInfo = VkPipelineDepthStencilStateCreateInfo.callocStack(stack)
            depthInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)

            val blendInfo = VkPipelineColorBlendStateCreateInfo.callocStack(stack)
            blendInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            blendInfo.attachments = colorAttachment

            val dynStates = VkDynamicStateBuffer(listOf(VkDynamicState.VIEWPORT, VkDynamicState.SCISSOR))

            val dynamicState = VkPipelineDynamicStateCreateInfo.callocStack(stack)
            dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            dynamicState.dynamicStates = dynStates

            val info = VkGraphicsPipelineCreateInfo.callocStack(1, stack)
            info[0].sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            info[0].flags = PIPELINE_CREATE_FLAGS
            info[0].stages = stage
            info[0].vertexInputState = vertexInfo
            info[0].inputAssemblyState = iaInfo
            info[0].viewportState = viewportInfo
            info[0].rasterizationState = rasterInfo
            info[0].multisampleState = msInfo
            info[0].depthStencilState = depthInfo
            info[0].colorBlendState = blendInfo
            info[0].dynamicState = dynamicState
            info[0].layout = pipelineLayout
            info[0].renderPass = renderPass

            err = vkCreateGraphicsPipelines(device, pipelineCache.L, info, allocator, lb).vkr
            checkVkResult(err)

            pipeline = VkPipeline(lb[0])

            vkDestroyShaderModule(device, vertInfo.L, allocator)
            vkDestroyShaderModule(device, fragInfo.L, allocator)
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
        val (pixels, size) = io.fonts.getTexDataAsRGBA32()

        val uploadSize = VkDeviceSize(size.x * size.y * 4 * Byte.BYTES.L)

        var err: VkResult

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val info = VkImageCreateInfo.callocStack(stack)
            info.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
            info.imageType = VkImageType._2D
            info.format = VkFormat.R8G8B8A8_UNORM
            info.extent.width = size.x
            info.extent.height = size.y
            info.extent.depth = 1
            info.mipLevels = 1
            info.arrayLayers = 1
            info.samples = VkSampleCount._1_BIT
            info.tiling = VkImageTiling.OPTIMAL
            info.usage = VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT
            info.sharingMode = VkSharingMode.EXCLUSIVE
            info.initialLayout = VkImageLayout.UNDEFINED
            err = vkCreateImage(device, info, allocator, lb).vkr
            checkVkResult(err)
            fontImage = VkImage(lb[0])

            val req = VkMemoryRequirements.callocStack(stack)
            vkGetImageMemoryRequirements(device, fontImage.L, req)

            val allocInfo = VkMemoryAllocateInfo.callocStack(stack)
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            allocInfo.allocationSize = req.size
            allocInfo.memoryTypeIndex = memoryType(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, req.memoryTypeBits)
            err = vkAllocateMemory(device, allocInfo, allocator, lb).vkr
            checkVkResult(err)
            fontMemory = VkDeviceMemory(lb[0])
            err = vkBindImageMemory(device, fontImage.L, fontMemory.L, 0).vkr
            checkVkResult(err)
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val info = VkImageViewCreateInfo.callocStack(stack)
            info.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
            info.image = fontImage
            info.viewType = VkImageViewType._2D
            info.format = VkFormat.R8G8B8A8_UNORM
            info.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            info.subresourceRange.levelCount = 1
            info.subresourceRange.layerCount = 1
            err = vkCreateImageView(device, info, allocator, lb).vkr
            checkVkResult(err)
            fontView = VkImageView(lb[0])
        }

        stackPush().let { stack ->
            val descImage = VkDescriptorImageInfo.callocStack(1, stack)
            descImage[0].sampler = fontSampler
            descImage[0].imageView = fontView
            descImage[0].imageLayout = VkImageLayout.SHADER_READ_ONLY_OPTIMAL

            val writeDesc = VkWriteDescriptorSet.callocStack(1, stack)
            writeDesc[0].sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            writeDesc[0].dstSet = descriptorSet
            writeDesc[0].descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER
            writeDesc[0].imageInfo = descImage
            vkUpdateDescriptorSets(device, writeDesc, null)
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val bufferInfo = VkBufferCreateInfo.callocStack(stack)
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            bufferInfo.size = uploadSize
            bufferInfo.usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT
            bufferInfo.sharingMode = VkSharingMode.EXCLUSIVE
            err = vkCreateBuffer(device, bufferInfo, allocator, lb).vkr
            checkVkResult(err)
            uploadBuffer = VkBuffer(lb[0])

            val req = VkMemoryRequirements.callocStack(stack)
            vkGetBufferMemoryRequirements(device, uploadBuffer.L, req)

            bufferMemoryAlignment = if (bufferMemoryAlignment.L > req.alignment.L) bufferMemoryAlignment else req.alignment

            val allocInfo = VkMemoryAllocateInfo.callocStack(stack)
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            allocInfo.allocationSize = req.size
            allocInfo.memoryTypeIndex = memoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, req.memoryTypeBits)
            err = vkAllocateMemory(device, allocInfo, allocator, lb).vkr
            checkVkResult(err)
            uploadBufferMemory = VkDeviceMemory(lb[0])
            err = vkBindBufferMemory(device, uploadBuffer.L, uploadBufferMemory.L, 0).vkr
            checkVkResult(err)
        }

        stackPush().let { stack ->
            val pb = stack.mallocPointer(1)
            err = vkMapMemory(device, uploadBufferMemory.L, 0, uploadSize.L, 0, pb).vkr
            checkVkResult(err)
            pixels.copyTo(pb[0])
            val range = VkMappedMemoryRange.callocStack(1, stack)
            range[0].sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
            range[0].memory = uploadBufferMemory
            range[0].size = uploadSize
            err = vkFlushMappedMemoryRanges(device, range).vkr
            checkVkResult(err)
            vkUnmapMemory(device, pb[0])
        }

        stackPush().let { stack ->
            val copyBarrier = VkImageMemoryBarrier.callocStack(1, stack)
            copyBarrier[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            copyBarrier[0].dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT
            copyBarrier[0].oldLayout = VkImageLayout.UNDEFINED
            copyBarrier[0].newLayout = VkImageLayout.TRANSFER_DST_OPTIMAL
            copyBarrier[0].srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
            copyBarrier[0].dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
            copyBarrier[0].image = fontImage
            copyBarrier[0].subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            copyBarrier[0].subresourceRange.levelCount = 1
            copyBarrier[0].subresourceRange.layerCount = 1
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, copyBarrier)

            val region = VkBufferImageCopy.callocStack(1, stack)
            region[0].imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            region[0].imageSubresource.layerCount = 1
            region[0].imageExtent.width = size.x
            region[0].imageExtent.height = size.y
            region[0].imageExtent.depth = 1
            vkCmdCopyBufferToImage(commandBuffer, uploadBuffer.L, fontImage.L, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

            val useBarrier = VkImageMemoryBarrier.callocStack(1, stack)
            useBarrier[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            useBarrier[0].srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT
            useBarrier[0].dstAccessMask = VK_ACCESS_SHADER_READ_BIT
            useBarrier[0].oldLayout = VkImageLayout.TRANSFER_DST_OPTIMAL
            useBarrier[0].newLayout = VkImageLayout.SHADER_READ_ONLY_OPTIMAL
            useBarrier[0].srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
            useBarrier[0].dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
            useBarrier[0].image = fontImage
            useBarrier[0].subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            useBarrier[0].subresourceRange.levelCount = 1
            useBarrier[0].subresourceRange.layerCount = 1
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, useBarrier)
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
            err = vkMapMemory(device, fd.vertexBufferMemory.L, 0, vertex_size.L, 0, pb).vkr
            checkVkResult(err)
            val vtxDst = MemoryUtil.memByteBuffer(pb[0], vertex_size)
            err = vkMapMemory(device, fd.indexBufferMemory.L, 0, index_size.L, 0, pb).vkr
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
            vkUnmapMemory(device, fd.vertexBufferMemory.L)
            vkUnmapMemory(device, fd.indexBufferMemory.L)
        }

        run {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.L)
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout.L, 0, longArrayOf(descriptorSet.L), null)
        }

        run {
            vkCmdBindVertexBuffers(commandBuffer, 0, longArrayOf(fd.vertexBuffer.L), longArrayOf(0))
            vkCmdBindIndexBuffer(commandBuffer, fd.indexBuffer.L, 0, VK_INDEX_TYPE_UINT16)
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
            vkCmdPushConstants(commandBuffer, pipelineLayout.L, VK_SHADER_STAGE_VERTEX_BIT, Float.BYTES * 0, scale)
            vkCmdPushConstants(commandBuffer, pipelineLayout.L, VK_SHADER_STAGE_VERTEX_BIT, Float.BYTES * 2, translate)
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
            vkDestroyBuffer(device, uploadBuffer.L, allocator)
            uploadBuffer = VkBuffer.NULL
        }
        if (uploadBufferMemory.isValid) {
            vkFreeMemory(device, uploadBufferMemory.L, allocator)
            uploadBufferMemory = VkDeviceMemory.NULL
        }
    }

    override fun destroyDeviceObjects() {
        destroyFontObjects()

        for (fd in framesDataBuffers) {
            if (fd.vertexBuffer.isValid) {
                vkDestroyBuffer(device, fd.vertexBuffer.L, allocator)
                fd.vertexBuffer = VkBuffer.NULL
            }
            if (fd.vertexBufferMemory.isValid) {
                vkFreeMemory(device, fd.vertexBufferMemory.L, allocator)
                fd.vertexBufferMemory = VkDeviceMemory.NULL
            }

            if (fd.indexBuffer.isValid) {
                vkDestroyBuffer(device, fd.indexBuffer.L, allocator)
                fd.indexBuffer = VkBuffer.NULL
            }
            if (fd.indexBufferMemory.isValid) {
                vkFreeMemory(device, fd.indexBufferMemory.L, allocator)
                fd.indexBufferMemory = VkDeviceMemory.NULL
            }
        }

        if (fontView.isValid) {
            vkDestroyImageView(device, fontView.L, allocator)
            fontView = VkImageView.NULL
        }

        if (fontImage.isValid) {
            vkDestroyImage(device, fontImage.L, allocator)
            fontImage = VkImage.NULL
        }

        if (fontMemory.isValid) {
            vkFreeMemory(device, fontMemory.L, allocator)
            fontMemory = VkDeviceMemory.NULL
        }

        if (fontSampler.isValid) {
            vkDestroySampler(device, fontSampler.L, allocator)
            fontSampler = VkSampler.NULL
        }

        if (descriptorSetLayout.isValid) {
            vkDestroyDescriptorSetLayout(device, descriptorSetLayout.L, allocator)
            descriptorSetLayout = VkDescriptorSetLayout.NULL
        }

        if (pipelineLayout.isValid) {
            vkDestroyPipelineLayout(device, pipelineLayout.L, allocator)
            pipelineLayout = VkPipelineLayout.NULL
        }

        if (pipeline.isValid) {
            vkDestroyPipeline(device, pipeline.L, allocator)
            pipeline = VkPipeline.NULL
        }
    }

    fun memoryType(properties: VkMemoryPropertyFlags, typeBits: Int): Int {
        stackPush().let { stack ->
            val props = VkPhysicalDeviceMemoryProperties.callocStack(stack)
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, props)
            for (i in 0 until props.memoryTypeCount()) {
                if ((props.memoryTypes(i).propertyFlags() and properties) == properties && (typeBits and (1 shl i)) != 0)
                    return i
            }
        }
        return -1
    }

    fun createOrResizeBuffer(buffer: VkBuffer, bufferMemory: VkDeviceMemory, newSize: VkDeviceSize, usage: VkBufferUsageFlags): Triple<VkBuffer, VkDeviceMemory, VkDeviceSize> {
        var err: VkResult

        if (buffer.isValid)
            vkDestroyBuffer(device, buffer.L, allocator)
        if (bufferMemory.isValid)
            vkFreeMemory(device, bufferMemory.L, allocator)

        val retBuffer: VkBuffer
        val retMem: VkDeviceMemory

        val vertexBufferSizeAligned = VkDeviceSize(((newSize.L - 1) / bufferMemoryAlignment.L + 1) * bufferMemoryAlignment.L)
        stackPush().let { stack ->
            val bufferInfo = VkBufferCreateInfo.callocStack(stack)
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            bufferInfo.size(vertexBufferSizeAligned.L)
            bufferInfo.usage(usage)
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            val lb = stack.mallocLong(1)
            err = vkCreateBuffer(device, bufferInfo, allocator, lb).vkr
            checkVkResult(err)
            retBuffer = VkBuffer(lb[0])

            val req = VkMemoryRequirements.mallocStack(stack)
            vkGetBufferMemoryRequirements(device, retBuffer.L, req)
            bufferMemoryAlignment = if (bufferMemoryAlignment.L > req.alignment.L) bufferMemoryAlignment else req.alignment
            val allocInfo = VkMemoryAllocateInfo.callocStack(stack)
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            allocInfo.allocationSize(req.size())
            allocInfo.memoryTypeIndex(memoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, req.memoryTypeBits))
            err = vkAllocateMemory(device, allocInfo, allocator, lb).vkr
            checkVkResult(err)
            retMem = VkDeviceMemory(lb[0])
        }

        err = vkBindBufferMemory(device, retBuffer.L, retMem.L, 0).vkr
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
            val ib = stack.mallocInt(1)
            for (i in 0 until IMGUI_VK_QUEUED_FRAMES) {
                val fd = wd.frames[i]

                run {
                    val info = VkCommandPoolCreateInfo.callocStack(stack)
                    info.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    info.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
                    info.queueFamilyIndex = queueFamily
                    err = vkCreateCommandPool(device, info, allocator, lb).vkr
                    checkVkResult(err)
                    fd.commandPool = VkCommandPool(lb[0])
                }

                run {
                    val info = VkCommandBufferAllocateInfo.callocStack(stack)
                    info.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    info.commandPool = fd.commandPool
                    info.level = VkCommandBufferLevel.PRIMARY
                    info.commandBufferCount = 1
                    err = vkAllocateCommandBuffers(device, info, pb).vkr
                    checkVkResult(err)
                    fd.commandBuffer = VkCommandBuffer(pb[0], device)
                }

                run {
                    val info = VkFenceCreateInfo.callocStack(stack)
                    info.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    info.flags = VK_FENCE_CREATE_SIGNALED_BIT
                    err = vkCreateFence(device, info, allocator, lb).vkr
                    checkVkResult(err)
                    fd.fence = VkFence(lb[0])
                }

                run {
                    val info = VkSemaphoreCreateInfo.callocStack(stack)
                    info.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
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
            vkDestroyFence(device, fd.fence.L, allocator)
            vkFreeCommandBuffers(device, fd.commandPool.L, fd.commandBuffer)
            vkDestroyCommandPool(device, fd.commandPool.L, allocator)
            vkDestroySemaphore(device, fd.imageAcquiredSemaphore.L, allocator)
            vkDestroySemaphore(device, fd.renderCompleteSemaphore.L, allocator)
        }

        for (i in 0 until wd.backbufferCount) {
            vkDestroyImageView(device, wd.backBufferView[i].L, allocator)
            vkDestroyFramebuffer(device, wd.framebuffer[i].L, allocator)
        }

        vkDestroyRenderPass(device, wd.renderPass.L, allocator)
        vkDestroySwapchainKHR(device, wd.swapchain.L, allocator)
        vkDestroySurfaceKHR(instance, wd.surface.L, allocator)

    }

    private fun createWindowDataSwapChainAndFramebuffer(physicalDevice: VkPhysicalDevice, device: VkDevice, wd: WindowData, allocator: VkAllocationCallbacks, w: Int, h: Int) {
        var minImageCount = 2

        val oldSwapchain = wd.swapchain
        var err = vkDeviceWaitIdle(device).vkr
        checkVkResult(err)

        for (i in 0 until wd.backbufferCount) {
            if (wd.backBufferView[i].isValid)
                vkDestroyImageView(device, wd.backBufferView[i].L, allocator)
            if (wd.framebuffer[i].isValid)
                vkDestroyFramebuffer(device, wd.framebuffer[i].L, allocator)
        }
        wd.backbufferCount = 0
        if (wd.renderPass.isValid)
            vkDestroyRenderPass(device, wd.renderPass.L, allocator)

        if (minImageCount == 0)
            minImageCount = getMinImageCountFromPresentMode(wd.presentMode)

        stackPush().let { stack ->
            val info = VkSwapchainCreateInfoKHR.callocStack(stack)
            info.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
            info.surface = wd.surface
            info.minImageCount = minImageCount
            info.imageFormat = wd.surfaceFormat.format
            info.imageColorSpace = wd.surfaceFormat.colorSpace
            info.imageArrayLayers = 1
            info.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
            info.imageSharingMode = VkSharingMode.EXCLUSIVE           // Assume that graphics family == present family
            info.preTransform = VkSurfaceTransformKHR.IDENTITY_BIT_KHR
            info.compositeAlpha = VkCompositeAlphaKHR.OPAQUE_BIT_KHR
            info.presentMode = wd.presentMode
            info.clipped = true
            info.oldSwapchain = oldSwapchain

            val cap = VkSurfaceCapabilitiesKHR.callocStack(stack)
            err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, wd.surface.L, cap).vkr
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
            val attachment = VkAttachmentDescription.callocStack(stack)
            attachment.format = wd.surfaceFormat.format
            attachment.samples = VkSampleCount._1_BIT
            attachment.loadOp = if (wd.clearEnable) VkAttachmentLoadOp.CLEAR else VkAttachmentLoadOp.DONT_CARE
            attachment.storeOp = VkAttachmentStoreOp.STORE
            attachment.stencilLoadOp = VkAttachmentLoadOp.DONT_CARE
            attachment.stencilStoreOp = VkAttachmentStoreOp.DONT_CARE
            attachment.initialLayout = VkImageLayout.UNDEFINED
            attachment.finalLayout = VkImageLayout.PRESENT_SRC_KHR
            val colorAttachment = VkAttachmentReference.callocStack(stack)
            colorAttachment.attachment = 0
            colorAttachment.layout = VkImageLayout.COLOR_ATTACHMENT_OPTIMAL
            val subpass = VkSubpassDescription.callocStack(stack)
            subpass.pipelineBindPoint = VkPipelineBindPoint.GRAPHICS
            subpass.colorAttachmentCount = 1
            subpass.colorAttachment = colorAttachment
            val dependency = VkSubpassDependency.callocStack(stack)
            dependency.srcSubpass = VK_SUBPASS_EXTERNAL
            dependency.dstSubpass = 0
            dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
            dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
            dependency.srcAccessMask = 0
            dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
            val info = VkRenderPassCreateInfo.callocStack(stack)
            info.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            info.attachment = attachment
            info.subpass = subpass
            info.dependency = dependency
            err = vkCreateRenderPass(device, info, allocator, lb).vkr
            checkVkResult(err)
        }

        stackPush().let { stack ->
            val lb = stack.mallocLong(1)
            val info = VkImageViewCreateInfo.callocStack(stack)
            info.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
            info.viewType = VkImageViewType._2D
            info.format = wd.surfaceFormat.format
            info.components.r = VkComponentSwizzle.R
            info.components.g = VkComponentSwizzle.G
            info.components.b = VkComponentSwizzle.B
            info.components.a = VkComponentSwizzle.A
            val imageRange = VkImageSubresourceRange.callocStack(stack)
            imageRange.set(VkImageAspect.COLOR_BIT.i, 0, 1, 0, 1)
            info.subresourceRange = imageRange
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
            val info = VkFramebufferCreateInfo.callocStack(stack)
            info.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
            info.renderPass = wd.renderPass
            info.attachment = attachment
            info.width = wd.width
            info.height = wd.height
            info.layers = 1
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
