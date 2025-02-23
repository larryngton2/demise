package wtf.demise.features.modules;

import wtf.demise.Demise;
import wtf.demise.utils.InstanceAccess;

//todo
public class Mode<N extends Module> implements InstanceAccess {
    public <M extends Module> M getModule(Class<M> clazz) {
        return Demise.INSTANCE.getModuleManager().getModule(clazz);
    }
}
