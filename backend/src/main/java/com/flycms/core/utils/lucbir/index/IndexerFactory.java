package com.flycms.core.utils.lucbir.index;

import com.flycms.core.utils.lucbir.features.*;
import com.flycms.core.utils.lucbir.features.*;

/**
 * Created by Administrator on 2016/7/25.
 */
public class IndexerFactory {

    public static LucbirIndexer createACLHIndexer(){
        return new LucbirIndexer(new AnnularColorLayoutHistogram());
    }

    public static LucbirIndexer createCCVIndexer(){
        return new LucbirIndexer(new ColorCoherenceVector());
    }

    public static LucbirIndexer createHSVHIndexer(){
        return new LucbirIndexer(new HSVColorHistogram());
    }

    public static LucbirIndexer createPHashIndexer(){
        return new LucbirIndexer(new PHash());
    }

    public static LucbirIndexer createRGBHIndexer(){
        return new LucbirIndexer(new RGBColorHistogram());
    }

    public static LucbirIndexer createRHashIndexer(){
        return new LucbirIndexer(new RHash());
    }

    public static LucbirIndexer createULBPIndexer(){
        return new LucbirIndexer(new UniformLBP());
    }
}
