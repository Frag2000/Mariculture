package mariculture.core.items;

import java.util.HashMap;
import java.util.List;

import mariculture.api.core.MaricultureRegistry;
import mariculture.api.core.MaricultureTab;
import mariculture.core.Core;
import mariculture.core.Mariculture;
import mariculture.core.helpers.BlockHelper;
import mariculture.core.helpers.FluidHelper;
import mariculture.core.helpers.cofh.StringHelper;
import mariculture.core.lib.Text;
import mariculture.core.util.IItemRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFluid;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemFluidStorage extends Item implements IFluidContainerItem, IItemRegistry {	
	public int capacity;
	private Icon filledIcon;
	public ItemFluidStorage(int i, int capacity) {
		super(i);
		this.capacity = capacity;
		setCreativeTab(MaricultureTab.tabMariculture);
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack item, World world, EntityPlayer player) {
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(world, player, true);

        if (movingobjectposition == null) {
            return item;
        } else {
            if (movingobjectposition.typeOfHit == EnumMovingObjectType.TILE) {
            	IFluidContainerItem container = ((IFluidContainerItem) item.getItem());
            	int x = movingobjectposition.blockX;
                int y = movingobjectposition.blockY;
                int z = movingobjectposition.blockZ;
                int side = movingobjectposition.sideHit;
            	
            	Block block = Block.blocksList[world.getBlockId(x, y, z)];
				if((block instanceof BlockFluidBase || block instanceof BlockFluid)) {
					FluidStack fluid = null;
					if(block instanceof BlockFluidBase)
						fluid = ((BlockFluidBase) block).drain(world, x, y, z, false);
					if(BlockHelper.isWater(world, x, y, z))
						fluid = FluidRegistry.getFluidStack("water", 1000);
					if(BlockHelper.isLava(world, x, y, z))
						fluid = FluidRegistry.getFluidStack("lava", 1000);
					if(fluid != null) {
						if(container.fill(item, fluid, false) >= fluid.amount) {
							if(block instanceof BlockFluidBase)
								((BlockFluidBase) block).drain(world, x, y, z, true);
							else
								world.setBlockToAir(x, y, z);
							container.fill(item, fluid, true);
							return item;
						}
					}
				} 
				
				if(((IFluidContainerItem) item.getItem()).getFluid(item) != null) {
					FluidStack stack = ((IFluidContainerItem) item.getItem()).getFluid(item);
					Fluid fluid = stack.getFluid();
					if(!fluid.canBePlacedInWorld()) {
						return item;
					}
					
					int drain = FluidHelper.getRequiredVolumeForBlock(fluid);
					FluidStack result = ((IFluidContainerItem) item.getItem()).drain(item, drain, false);
					if(result == null || result.amount < drain) {
						return item;
					}
					
					if(!result.getFluid().getName().equals("lava") && world.provider.isHellWorld) {
						return item;
					}
					
					int i1 = world.getBlockId(x, y, z);

					Block aBlock = Block.blocksList[i1];
					
					if(side == 1 && (aBlock instanceof BlockFluidBase || aBlock instanceof BlockFluid)) {
						--y;
					} else if (i1 == Block.snow.blockID && (world.getBlockMetadata(x, y, z) & 7) < 1) {
						side = 1;
					} else if (i1 != Block.vine.blockID && i1 != Block.tallGrass.blockID && i1 != Block.deadBush.blockID) {
						if (side == 0)
							--y;
						if (side == 1)
							++y;
						if (side == 2)
							--z;
						if (side == 3)
							++z;
						if (side == 4)
							--x;
						if (side == 5)
							++x;
					}

					if (!player.canPlayerEdit(x, y, z, side, item)) {
						return item;
					} else if (item.stackSize == 0) {
						return item;
					} else {
						Block theBlock = Block.blocksList[fluid.getBlockID()];
						if (world.setBlock(x, y, z, fluid.getBlockID(), 0, 2)) {
							if (world.getBlockId(x, y, z) == fluid.getBlockID()) {
								Block.blocksList[fluid.getBlockID()].onBlockPlacedBy(world, x, y, z, player, item);
								Block.blocksList[fluid.getBlockID()].onPostBlockPlaced(world, x, y, z, 0);
							}

							world.playSoundEffect((double) ((float) x + 0.5F), (double) ((float) y + 0.5F),
									(double) ((float) z + 0.5F), theBlock.stepSound.getPlaceSound(),
									(theBlock.stepSound.getVolume() + 1.0F) / 2.0F, theBlock.stepSound.getPitch() * 0.8F);
							
							((IFluidContainerItem) item.getItem()).drain(item, drain, true);
							return item;
						}
					}
				}
            }
        }
        
        return item;
    }
	
	@Override
	public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		if(FluidHelper.isIContainer(item)) {
			if (world.getBlockTileEntity(x, y, z) instanceof IFluidHandler) {
				ForgeDirection dir = ForgeDirection.getOrientation(side);
				IFluidHandler tank = (IFluidHandler) world.getBlockTileEntity(x, y, z);
				IFluidContainerItem container = (IFluidContainerItem) item.getItem();
				FluidStack fluid = container.getFluid(item);
				if(fluid != null && fluid.amount > 0) {
					FluidStack stack = container.drain(item, 1000000, false);
					if(stack != null) {
						stack.amount = tank.fill(dir, stack, false);					
						if(stack.amount > 0) {
							container.drain(item, stack.amount, true);
							tank.fill(dir, stack, true);
							return true;
						}
					}
				} else {
					FluidStack stack = tank.drain(dir, capacity, false);
					if(stack != null && stack.amount > 0) {
						tank.drain(dir, stack.amount, true);
						container.fill(item, stack, true);
						return true;
					}
				}
			}
		}
		

		return false;
	}

	@Override
	public boolean shouldPassSneakingClickToBlock(World world, int x, int y, int z) {
		return true;
	}
	
	@Override
	public String getItemDisplayName(ItemStack stack) {
        return Text.ORANGE + ("" + StatCollector.translateToLocal(getUnlocalizedNameInefficiently(stack) + ".name")).trim();
    }

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		FluidStack fluid = getFluid(stack);
		int amount = fluid == null? 0: fluid.amount;
		list.add(StringHelper.getFluidName(fluid));
		list.add("" + amount + "/" + capacity +  "mB");
	}
	
	@Override
	public boolean requiresMultipleRenderPasses() {
		return true;
	}
	
	@Override
	public Icon getIcon(ItemStack stack, int pass) {
		if(stack.itemID == Core.bucket.itemID) {
			if(pass == 0) {
				if(stack.hasTagCompound() && getFluid(stack) != null) {
					ItemStack bucket = FluidContainerRegistry.fillFluidContainer(getFluid(stack), new ItemStack(Item.bucketEmpty));
					if(bucket != null) {
						return bucket.getIconIndex();
					}
				}
				
				return itemIcon;
			} else {
				return filledIcon;
			}
		}
		
		return itemIcon;
	}
	 
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister) {
		itemIcon = iconRegister.registerIcon(Mariculture.modid + ":" + getName(new ItemStack(this)));
		if(this.itemID == Core.bucket.itemID)
			filledIcon = iconRegister.registerIcon(Mariculture.modid + ":" + getName(new ItemStack(this)) + "Filled");
	}

	@Override
	public int getCapacity(ItemStack stack) {
		return capacity;
	}

	@Override
	public FluidStack getFluid(ItemStack container) {
		if (container.stackTagCompound == null || !container.stackTagCompound.hasKey("Fluid")) {
			return null;
		}

		return FluidStack.loadFluidStackFromNBT(container.stackTagCompound.getCompoundTag("Fluid"));
	}

	@Override
	public int fill(ItemStack container, FluidStack resource, boolean doFill) {
		if (resource == null) {
			return 0;
		}

		if (!doFill) {
			if (container.stackTagCompound == null || !container.stackTagCompound.hasKey("Fluid")) {
				return Math.min(capacity, resource.amount);
			}

			FluidStack stack = FluidStack.loadFluidStackFromNBT(container.stackTagCompound.getCompoundTag("Fluid"));

			if (stack == null) {
				return Math.min(capacity, resource.amount);
			}

			if (!stack.isFluidEqual(resource) && stack.amount > 0) {
				return 0;
			}

			return Math.min(capacity - stack.amount, resource.amount);
		}

		if (container.stackTagCompound == null) {
			container.stackTagCompound = new NBTTagCompound();
		}

		if (!container.stackTagCompound.hasKey("Fluid")) {
			NBTTagCompound fluidTag = resource.writeToNBT(new NBTTagCompound());

			if (capacity < resource.amount) {
				fluidTag.setInteger("Amount", capacity);
				container.stackTagCompound.setTag("Fluid", fluidTag);
				return capacity;
			}

			container.stackTagCompound.setTag("Fluid", fluidTag);
			return resource.amount;
		}

		NBTTagCompound fluidTag = container.stackTagCompound.getCompoundTag("Fluid");
		FluidStack stack = FluidStack.loadFluidStackFromNBT(fluidTag);

		if (!stack.isFluidEqual(resource) && stack.amount > 0) {
			return 0;
		}
		
		if(!stack.isFluidEqual(resource)) {
			stack.fluidID = resource.fluidID;
		}

		int filled = capacity - stack.amount;
		if (resource.amount < filled) {
			stack.amount += resource.amount;
			filled = resource.amount;
		} else {
			stack.amount = capacity;
		}

		container.stackTagCompound.setTag("Fluid", stack.writeToNBT(fluidTag));
		return filled;
	}

	@Override
	public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
		if (container.stackTagCompound == null || !container.stackTagCompound.hasKey("Fluid")) {
			return null;
		}
		
		FluidStack stack = FluidStack.loadFluidStackFromNBT(container.stackTagCompound.getCompoundTag("Fluid"));
		if (stack == null) {
			return null;
		}

		stack.amount = Math.min(stack.amount, maxDrain);
		if (doDrain) {
			NBTTagCompound fluidTag = container.stackTagCompound.getCompoundTag("Fluid");
			int newAmount = fluidTag.getInteger("Amount") - stack.amount;
			if(newAmount == 0) {
				container.setTagCompound(null);
				return stack;
			}
			
			fluidTag.setInteger("Amount", newAmount);
			container.stackTagCompound.setTag("Fluid", fluidTag);
		}
		
		return stack;
	}

	@Override
	public void register() {
		MaricultureRegistry.register(getName(new ItemStack(this.itemID, 1, 0)), new ItemStack(this.itemID, 1, 0));
	}

	@Override
	public int getMetaCount() {
		return 1;
	}

	@Override
	public String getName(ItemStack stack) {
		return this.getUnlocalizedName().substring(5);
	}
}
