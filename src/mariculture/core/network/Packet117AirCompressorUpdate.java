package mariculture.core.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mariculture.diving.TileAirCompressor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class Packet117AirCompressorUpdate extends Packet110CustomTileUpdate {
	protected int air;
	
	public Packet117AirCompressorUpdate() {}
	
	public Packet117AirCompressorUpdate(int x, int y, int z, int air) {
		super(x, y, z);
		this.air = air;
	}
	
	@Override
	public void handle(World world, EntityPlayer player) {
		super.handle(world, player);
		
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if(tile != null && tile instanceof TileAirCompressor) {
			((TileAirCompressor)tile).storedAir = air;
		}
	}

	@Override
	public void read(DataInputStream is) throws IOException {
		super.read(is);
		air = is.readInt();
	}

	@Override
	public void write(DataOutputStream os) throws IOException {
		super.write(os);
		os.writeInt(air);
	}
}
