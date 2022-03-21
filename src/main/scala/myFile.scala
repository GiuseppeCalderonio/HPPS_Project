
package HPPS_Projcet
import java.io.ObjectInputFilter.Config

import freechips.rocketchip.config.{Config}


///////////////////// general fo the core

trait TileParams {
  val core: CoreParams                  // Core parameters (see below)
  val icache: Option[ICacheParams]      // Rocket specific: I1 cache option
  val dcache: Option[DCacheParams]      // Rocket specific: D1 cache option
  val btb: Option[BTBParams]            // Rocket specific: BTB / branch predictor option
  val hartId: Int                       // Hart ID: Must be unique within a design config (This MUST be a case class parameter)
  val beuAddr: Option[BigInt]           // Rocket specific: Bus Error Unit for Rocket Core
  val blockerCtrlAddr: Option[BigInt]   // Rocket specific: Bus Blocker for Rocket Core
  val name: Option[String]              // Name of the core
}

abstract class InstantiableTileParams[TileType <: BaseTile] extends TileParams {
  def instantiate(crossing: TileCrossingParamsLike, lookup: LookupByHartIdImpl)
                (implicit p: Parameters): TileType
}

trait CoreParams {
  val bootFreqHz: BigInt              // Frequency
  val useVM: Boolean                  // Support virtual memory
  val useUser: Boolean                // Support user mode
  val useSupervisor: Boolean          // Support supervisor mode
  val useDebug: Boolean               // Support RISC-V debug specs
  val useAtomics: Boolean             // Support A extension
  val useAtomicsOnlyForIO: Boolean    // Support A extension for memory-mapped IO (may be true even if useAtomics is false)
  val useCompressed: Boolean          // Support C extension
  val useVector: Boolean = false      // Support V extension
  val useSCIE: Boolean                // Support custom instructions (in custom-0 and custom-1)
  val useRVE: Boolean                 // Use E base ISA
  val mulDiv: Option[MulDivParams]    // *Rocket specific: M extension related setting (Use Some(MulDivParams()) to indicate M extension supported)
  val fpu: Option[FPUParams]          // F and D extensions and related setting (see below)
  val fetchWidth: Int                 // Max # of insts fetched every cycle
  val decodeWidth: Int                // Max # of insts decoded every cycle
  val retireWidth: Int                // Max # of insts retired every cycle
  val instBits: Int                   // Instruction bits (if 32 bit and 64 bit are both supported, use 64)
  val nLocalInterrupts: Int           // # of local interrupts (see SiFive interrupt cookbook)
  val nPMPs: Int                      // # of Physical Memory Protection units
  val pmpGranularity: Int             // Size of the smallest unit of region for PMP unit (must be power of 2)
  val nBreakpoints: Int               // # of hardware breakpoints supported (in RISC-V debug specs)
  val useBPWatch: Boolean             // Support hardware breakpoints
  val nPerfCounters: Int              // # of supported performance counters
  val haveBasicCounters: Boolean      // Support basic counters defined in the RISC-V counter extension
  val haveFSDirty: Boolean            // If true, the core will set FS field in mstatus CSR to dirty when appropriate
  val misaWritable: Boolean           // Support writable misa CSR (like variable instruction bits)
  val haveCFlush: Boolean             // Rocket specific: enables Rocket's custom instruction extension to flush the cache
  val nL2TLBEntries: Int              // # of L2 TLB entries
  val mtvecInit: Option[BigInt]       // mtvec CSR (of V extension) initial value
  val mtvecWritable: Boolean          // If mtvec CSR is writable

  // Normally, you don't need to change these values (except lrscCycles)
  def customCSRs(implicit p: Parameters): CustomCSRs = new CustomCSRs

  def hasSupervisorMode: Boolean = useSupervisor || useVM
  def instBytes: Int = instBits / 8
  def fetchBytes: Int = fetchWidth * instBytes
  // Rocket specific: Longest possible latency of Rocket core D1 cache. Simply set it to the default value 80 if you don't use it.
  def lrscCycles: Int

  def dcacheReqTagBits: Int = 6

  def minFLen: Int = 32
  def vLen: Int = 0
  def sLen: Int = 0
  def eLen(xLen: Int, fLen: Int): Int = xLen max fLen
  def vMemDataBits: Int = 0
}

case class FPUParams(
  minFLen: Int = 32,          // Minimum floating point length (no need to change)
  fLen: Int = 64,             // Maximum floating point length, use 32 if only single precision is supported
  divSqrt: Boolean = true,    // Div/Sqrt operation supported
  sfmaLatency: Int = 3,       // Rocket specific: Fused multiply-add pipeline latency (single precision)
  dfmaLatency: Int = 4        // Rocket specific: Fused multiply-add pipeline latency (double precision)
)


///////////////////////// specific for my core

case class MyTileAttachParams(
  tileParams: MyTileParams,
  crossingParams: RocketCrossingParams
) extends CanAttachTile {
  type TileType = MyTile
  val lookup = PriorityMuxHartIdFromSeq(Seq(tileParams))
}


class MyTile(
  val myParams: MyTileParams,
  crossing: ClockCrossingType,
  lookup: LookupByHartIdImpl,
  q: Parameters)
  extends BaseTile(myParams, crossing, lookup, q)
  with SinksExternalInterrupts
  with SourcesExternalNotifications
{

  // Private constructor ensures altered LazyModule.p is used implicitly
  def this(params: MyTileParams, crossing: TileCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters) =
    this(params, crossing.crossingType, lookup, p)

  // Require TileLink nodes
  val intOutwardNode = IntIdentityNode()
  val masterNode = visibilityNode
  val slaveNode = TLIdentityNode()

  // Implementation class (See below)
  override lazy val module = new MyTileModuleImp(this)

  // Required entry of CPU device in the device tree for interrupt purpose
  val cpuDevice: SimpleDevice = new SimpleDevice("cpu", Seq("my-organization,my-cpu", "riscv")) {
    override def parent = Some(ResourceAnchors.cpus)
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping ++
                        cpuProperties ++
                        nextLevelCacheProperty ++
                        tileProperties)
    }
  }

  ResourceBinding {
    Resource(cpuDevice, "reg").bind(ResourceAddress(hartId))
  }

  // Create TileLink nodes and connections.

  // tile link
  (tlMasterXbar.node  // tlMasterXbar is the bus crossbar to be used when this core / tile is acting as a master; otherwise, use tlSlaveXBar
  := memoryTap
  := TLBuffer()
  := TLFIFOFixer(TLFIFOFixer.all) // fix FIFO ordering
  := TLWidthWidget(masterPortBeatBytes) // reduce size of TL
  := AXI4ToTL() // convert to TL
  := AXI4UserYanker(Some(2)) // remove user field on AXI interface. need but in reality user intf. not needed
  := AXI4Fragmenter() // deal with multi-beat xacts
  := memAXI4Node) // The custom node, see below

      // # of bits used in TileLink ID for master node. 4 bits can support 16 master nodes, but you can have a longer ID if you need more.
  val idBits = 4
  val memAXI4Node = AXI4MasterNode(
    Seq(AXI4MasterPortParameters(
      masters = Seq(AXI4MasterParameters(
        name = "myPortName",
        id = IdRange(0, 1 << idBits))))))
  val memoryTap = TLIdentityNode() // Every bus connection should have their own tap node



  // By default, their value is "TLBuffer(BufferParams.none)".
protected def makeMasterBoundaryBuffers(implicit p: Parameters): TLBuffer
protected def makeSlaveBoundaryBuffers(implicit p: Parameters): TLBuffer

}


class MyTileModuleImp(outer: MyTile) extends BaseTileModuleImp(outer){
  // annotate the parameters
  Annotated.params(this, outer.myParams)

  // TODO: Create the top module of the core and connect it with the ports in "outer"
  // If your core is in Chisel, you can simply instantiate the top module here like other Chisel module
  // and connect appropriate signal. You can even implement this class as your top module.
  // See https://github.com/riscv-boom/riscv-boom/blob/master/src/main/scala/common/tile.scala and
  // https://github.com/chipsalliance/rocket-chip/blob/master/src/main/scala/tile/RocketTile.scala for
  // Chisel example.

  // top module creation

  val io = IO(new Bundle {
    val in  = Input(UInt(4.W))
    val out = Output(UInt(4.W))
  })

  io.out := RegNext(io.in + 1.U)


  // connect to core
   outer.memAXI4Node.out foreach { case (out, edgeOut) =>

    out.apply(new AXI4BundleParameters(0, io.out, 0, Nil, Nil, Nil))
     
    // Connect your module IO port to "out"
    // The type of "out" here is AXI4Bundle, which is defined in generators/rocket-chip/src/main/scala/amba/axi4/Bundles.scala
    // Please refer to this file for the definition of the ports.
    // If you are using APB, check APBBundle in generators/rocket-chip/src/main/scala/amba/apb/Bundles.scala
    // If you are using AHB, check AHBSlaveBundle or AHBMasterBundle in generators/rocket-chip/src/main/scala/amba/ahb/Bundles.scala
    // (choose one depends on the type of AHB node you create)
    // If you are using AXIS, check AXISBundle and AXISBundleBits in generators/rocket-chip/src/main/scala/amba/axis/Bundles.scala
  }

    // For example, our core support debug interrupt and machine-level interrupt, and suppose the following two signals
  // are the interrupt inputs to the core. (DO NOT COPY this code - if your core treat each type of interrupt differently,
  // you need to connect them to different interrupt ports of your core)
  val debug_i = Wire(Bool())
  val mtip_i = Wire(Bool())
  // We create a bundle here and decode the interrupt.
  val int_bundle = new TileInterrupts()
  outer.decodeCoreInterrupts(int_bundle)
  debug_i := int_bundle.debug
  mtip_i := int_bundle.meip & int_bundle.msip & int_bundle.mtip

}



class WithNMyCores(n: Int = 1, overrideIdOffset: Option[Int] = None) extends Config((site, here, up) => {
  case TilesLocated(InSubsystem) => {
    // Calculate the next available hart ID (since hart ID cannot be duplicated)
    val prev = up(TilesLocated(InSubsystem), site)
    val idOffset = overrideIdOffset.getOrElse(prev.size)
    // Create TileAttachParams for every core to be instantiated
    (0 until n).map { i =>
      MyTileAttachParams(
        tileParams = MyTileParams(hartId = i + idOffset),
        crossingParams = RocketCrossingParams()
      )
    } ++ prev
  }
  // Configurate # of bytes in one memory / IO transaction. For RV64, one load/store instruction can transfer 8 bytes at most.
  case SystemBusKey => up(SystemBusKey, site).copy(beatBytes = 8)
  // The # of instruction bits. Use maximum # of bits if your core supports both 32 and 64 bits.
  case XLen => 64
})

// comment

class SorcConfig extends Config(
  new WithNMyCores(1) ++  // add 1 rocket core
  new chipyard.config.AbstractConfig)