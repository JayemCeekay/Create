package com.simibubi.create.content.contraptions.components.deployer;

import static com.simibubi.create.content.contraptions.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.simibubi.create.content.contraptions.base.DirectionalKineticBlock.FACING;

import com.jozufozu.flywheel.backend.instancing.IDynamicInstance;
import com.jozufozu.flywheel.backend.instancing.ITickableInstance;
import com.jozufozu.flywheel.backend.material.MaterialManager;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.mojang.math.Quaternion;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.ShaftInstance;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;

public class DeployerInstance extends ShaftInstance implements IDynamicInstance, ITickableInstance {

    final DeployerTileEntity tile;
    final Direction facing;
    final float yRot;
    final float zRot;
    final float zRotPole;

    protected final OrientedData pole;

    protected OrientedData hand;

    PartialModel currentHand;
    float progress;
    private boolean newHand = false;

    public DeployerInstance(MaterialManager dispatcher, KineticTileEntity tile) {
        super(dispatcher, tile);

        this.tile = (DeployerTileEntity) super.tile;
        facing = blockState.getValue(FACING);

        boolean rotatePole = blockState.getValue(AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z;

        yRot = AngleHelper.horizontalAngle(facing);
        zRot = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
        zRotPole = rotatePole ? 90 : 0;

        pole = getOrientedMaterial().getModel(AllBlockPartials.DEPLOYER_POLE, blockState).createInstance();

        updateHandPose();
        relight(pos, pole);

        progress = getProgress(AnimationTickHolder.getPartialTicks());
        updateRotation(pole, hand, yRot, zRot, zRotPole);
        updatePosition();
    }

    @Override
    public void tick() {
        newHand = updateHandPose();
    }

    @Override
    public void beginFrame() {

        float newProgress = getProgress(AnimationTickHolder.getPartialTicks());

        if (!newHand && Mth.equal(newProgress, progress)) return;

        progress = newProgress;
        newHand = false;

        updatePosition();
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, hand, pole);
    }

    @Override
    public void remove() {
        super.remove();
        hand.delete();
        pole.delete();
    }

    private boolean updateHandPose() {
        PartialModel handPose = tile.getHandPose();

        if (currentHand == handPose) return false;
        currentHand = handPose;

        if (hand != null) hand.delete();

        hand = getOrientedMaterial().getModel(currentHand, blockState).createInstance();

        relight(pos, hand);
        updateRotation(pole, hand, yRot, zRot, zRotPole);
        updatePosition();

        return true;
    }

    private float getProgress(float partialTicks) {
        if (tile.state == DeployerTileEntity.State.EXPANDING)
            return 1 - (tile.timer - partialTicks * tile.getTimerSpeed()) / 1000f;
        if (tile.state == DeployerTileEntity.State.RETRACTING)
            return (tile.timer - partialTicks * tile.getTimerSpeed()) / 1000f;
        return 0;
    }

    private void updatePosition() {
        float handLength = currentHand == AllBlockPartials.DEPLOYER_HAND_POINTING ? 0
                : currentHand == AllBlockPartials.DEPLOYER_HAND_HOLDING ? 4 / 16f : 3 / 16f;
        float distance = Math.min(Mth.clamp(progress, 0, 1) * (tile.reach + handLength), 21 / 16f);
        Vec3i facingVec = facing.getNormal();
        BlockPos blockPos = getInstancePosition();

        float x = blockPos.getX() + ((float) facingVec.getX()) * distance;
        float y = blockPos.getY() + ((float) facingVec.getY()) * distance;
        float z = blockPos.getZ() + ((float) facingVec.getZ()) * distance;

        pole.setPosition(x, y, z);
        hand.setPosition(x, y, z);
    }

    static void updateRotation(OrientedData pole, OrientedData hand, float yRot, float zRot, float zRotPole) {

        Quaternion q = Direction.SOUTH.step().rotationDegrees(zRot);
        q.mul(Direction.UP.step().rotationDegrees(yRot));

        hand.setRotation(q);

        q.mul(Direction.SOUTH.step().rotationDegrees(zRotPole));

        pole.setRotation(q);
    }
}