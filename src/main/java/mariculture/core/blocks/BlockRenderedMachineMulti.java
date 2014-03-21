package mariculture.core.blocks;

import mariculture.Mariculture;
import mariculture.core.blocks.base.BlockFunctionalMulti;
import mariculture.core.helpers.FluidHelper;
import mariculture.core.helpers.SpawnItemHelper;
import mariculture.core.helpers.cofh.ItemHelper;
import mariculture.core.lib.MachineRenderedMultiMeta;
import mariculture.core.lib.Modules;
import mariculture.core.lib.RenderIds;
import mariculture.core.network.PacketCompressor;
import mariculture.core.network.Packets;
import mariculture.core.tile.TileVat;
import mariculture.diving.Diving;
import mariculture.diving.TileAirCompressor;
import mariculture.factory.tile.TilePressureVessel;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;
import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockRenderedMachineMulti extends BlockFunctionalMulti {
	public IIcon bar1;
	public BlockRenderedMachineMulti() {
		super(Material.iron);
		setLightOpacity(0);
	}
	
	@Override
	public String getToolType(int meta) {
		return "pickaxe";
	}

	@Override
	public int getToolLevel(int meta) {
		return meta == MachineRenderedMultiMeta.VAT? 1: 2;
	}
	
	@Override
	public float getBlockHardness(World world, int x, int y, int z) {
		switch (world.getBlockMetadata(x, y, z)) {
			case MachineRenderedMultiMeta.COMPRESSOR_BASE: 	return 6F;
			case MachineRenderedMultiMeta.COMPRESSOR_TOP: 	return 4F;
			case MachineRenderedMultiMeta.PRESSURE_VESSEL: 	return 15F;
			case MachineRenderedMultiMeta.VAT: 				return 2.5F;
			default:										return 5F;
		}
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile == null) {
			return false;
		}
		
		ItemStack heldItem = player.getCurrentEquippedItem();
		if(heldItem != null && tile instanceof TileAirCompressor) {
			TileAirCompressor te = (TileAirCompressor) ((TileAirCompressor) tile).getMaster();
			if(te != null) {
				int rf = (heldItem.getItem() instanceof IEnergyContainerItem)? 
						((IEnergyContainerItem)heldItem.getItem()).extractEnergy(heldItem, 5000, true): 0;
				if(rf > 0) {
					int drain = te.receiveEnergy(ForgeDirection.UP, rf, true);
					if(drain > 0) {
						((IEnergyContainerItem)heldItem.getItem()).extractEnergy(heldItem, drain, false);
						te.receiveEnergy(ForgeDirection.UP, drain, false);
					}
					
					return true;
				}
				
				if(heldItem.getItem() == Diving.scubaTank) {
					if(heldItem.getItemDamage() > 1 && te.storedAir > 0) {
						heldItem.setItemDamage(heldItem.getItemDamage() - 1);
						if(!world.isRemote) {
							te.storedAir--;
							Packets.updateAround(te, new PacketCompressor(te.xCoord, te.yCoord, te.zCoord,  te.storedAir, te.getEnergyStored(ForgeDirection.UP)));
						}
						return true;
					}
				}
			} else return false;
		}
		
		if(tile instanceof TileVat) {
			TileVat vat = (TileVat) tile;
			ItemStack held = player.getCurrentEquippedItem();
			ItemStack input = vat.getStackInSlot(0);
			ItemStack output = vat.getStackInSlot(1);
			if(FluidHelper.isFluidOrEmpty(player.getCurrentEquippedItem())) {
				return FluidHelper.handleFillOrDrain((IFluidHandler) world.getTileEntity(x, y, z), player, ForgeDirection.UP);
			}
			
			if(output != null) {
				if (!player.inventory.addItemStackToInventory(vat.getStackInSlot(1))) {
					if(!world.isRemote) {
						SpawnItemHelper.spawnItem(world, x, y + 1, z, vat.getStackInSlot(1));
					}
				}
				
				vat.setInventorySlotContents(1, null);
				
				return true;
			} else if(player.isSneaking() && input != null) {				
				if (!player.inventory.addItemStackToInventory(vat.getStackInSlot(0))) {
					if(!world.isRemote) {
						SpawnItemHelper.spawnItem(world, x, y + 1, z, vat.getStackInSlot(0));
					}
				}
				
				vat.setInventorySlotContents(0, null);
								
				return true;
			} else if(held != null && !player.isSneaking()) {
				if(input == null) {
					if(!world.isRemote) {
						ItemStack copy = held.copy();
						copy.stackSize = 1;
						vat.setInventorySlotContents(0, copy);
						player.inventory.decrStackSize(player.inventory.currentItem, 1);
					}
					
					return true;
				} else if(ItemHelper.areItemStacksEqualNoNBT(input, held)) {
					if(input.stackSize + 1 < input.getMaxStackSize()) {
						if(!world.isRemote) {
							ItemStack stack = input.copy();
							stack.stackSize++;
							vat.setInventorySlotContents(0, stack);
							player.inventory.decrStackSize(player.inventory.currentItem, 1);
						}
						return true;
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		}
		
		return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		return RenderIds.RENDER_ALL;
	}
	
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess block, int x, int y, int z) {
		int meta = block.getBlockMetadata(x, y, z);
		switch (meta) {
		case MachineRenderedMultiMeta.COMPRESSOR_TOP:
			setBlockBounds(0.05F, 0F, 0.05F, 0.95F, 0.15F, 0.95F);
			break;
		case MachineRenderedMultiMeta.COMPRESSOR_BASE:
			setBlockBounds(0.05F, 0F, 0.05F, 0.95F, 1F, 0.95F);
			break;
		default:
			setBlockBounds(0F, 0F, 0F, 1F, 0.95F, 1F);
		}
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		if(world.getBlockMetadata(x, y, z) == MachineRenderedMultiMeta.VAT) {
			return AxisAlignedBB.getAABBPool().getAABB((double) x + this.minX, (double) y + this.minY,		
					(double) z + this.minZ, (double) x + this.maxX, (double) y + 0.50001F, (double) z + this.maxZ);
		}
		
		return AxisAlignedBB.getAABBPool().getAABB((double) x + this.minX, (double) y + this.minY,		
			(double) z + this.minZ, (double) x + this.maxX, (double) y + this.maxY, (double) z + this.maxZ);
	}

	@Override
	public boolean onBlockDropped(World world, int x, int y, int z) {
		return false;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		switch (meta) {
			case MachineRenderedMultiMeta.COMPRESSOR_BASE: 	return new TileAirCompressor();
			case MachineRenderedMultiMeta.COMPRESSOR_TOP: 	return new TileAirCompressor();
			case MachineRenderedMultiMeta.PRESSURE_VESSEL: 	return new TilePressureVessel();
			case MachineRenderedMultiMeta.VAT: 				return new TileVat();
			default:										return null;
		}
	}

	@Override
	public boolean isActive(int meta) {
		switch (meta) {
		case MachineRenderedMultiMeta.COMPRESSOR_BASE: 	return (Modules.isActive(Modules.diving));
		case MachineRenderedMultiMeta.COMPRESSOR_TOP: 	return (Modules.isActive(Modules.diving));
		case MachineRenderedMultiMeta.PRESSURE_VESSEL: 	return (Modules.isActive(Modules.factory));
		default: 										return true;
		}
	}
	
	@Override
	public int getMetaCount() {
		return MachineRenderedMultiMeta.COUNT;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		bar1 = iconRegister.registerIcon(Mariculture.modid + ":bar1");
		super.registerBlockIcons(iconRegister);
	}
}
