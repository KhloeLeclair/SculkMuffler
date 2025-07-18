package dev.khloeleclair.skulkmuffler.common.integrations.jade;

import dev.khloeleclair.skulkmuffler.common.blocks.MufflerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {

    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {

        registration.registerBlockComponent(new MufflerBlockProvider(), MufflerBlock.class);

    }
}
