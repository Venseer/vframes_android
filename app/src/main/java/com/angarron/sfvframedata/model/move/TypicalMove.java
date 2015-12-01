package com.angarron.sfvframedata.model.move;

/**
 * Created by andy on 11/30/15
 */
public class TypicalMove extends BaseDisplayableMove {

    private int startupFrames;
    private int activeFrames;
    private int recoveryFrames;

    private int blockstunFrames;
    private int hitstunFrames;

    private int damageValue;
    private int stunValue;

    private MoveStrength strength;

    public TypicalMove (String name, String label, MoveType moveType, int startupFrames,
                        int activeFrames, int recoveryFrames, int blockstunFrames, int hitstunFrames,
                        int damageValue, int stunValue, MoveStrength strength) {

        super(name, label, moveType);

        this.startupFrames = startupFrames;
        this.activeFrames = activeFrames;
        this.recoveryFrames = recoveryFrames;

        this.blockstunFrames = blockstunFrames;
        this.hitstunFrames = hitstunFrames;

        this.damageValue = damageValue;
        this.stunValue = stunValue;

        this.strength = strength;
    }

    @Override
    public int getStartupFrames() {
        return startupFrames;
    }

    @Override
    public int getActiveFrames() {
        return activeFrames;
    }

    @Override
    public int getRecoveryFrames() {
        return recoveryFrames;
    }

    @Override
    public int getBlockAdvantage() {
        return blockstunFrames - (recoveryFrames + activeFrames - 1);
    }

    @Override
    public int getHitAdvantage() {
        return hitstunFrames - (recoveryFrames + activeFrames - 1);
    }

    @Override
    public int getDamageValue() {
        return damageValue;
    }

    @Override
    public int getStunValue() {
        return stunValue;
    }

    @Override
    public MoveStrength getStrength() {
        return strength;
    }

    //TODO: write toJSON
}
