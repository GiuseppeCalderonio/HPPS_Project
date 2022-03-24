package HPPS_Project

class TorusTemplate extends Config((site, here, up) => {
    case BuildRoCC => Seq((p: Parameters) => {
            val torus_template = LazyModule.apply(new TorusAccelerator(OpcodeSet.custom0)(p))
            torus_template
        }
    )
  }
)

class TorusTemplateDefaultConfig extends Config(
    new TorusTemplate ++
    new freechips.rocketchip.subsystem.WithNBigCores(1)
)