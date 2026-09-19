package com.flycms.core.utils.lucbir.search;

import com.flycms.core.utils.lucbir.features.*;

/**
 * Created by Administrator on 2016/7/25.
 */
public class SearcherFactory {
    public static LucbirSearcher createACLHSearcher(){
        return new LucbirSearcher(new AnnularColorLayoutHistogram());
    }

    public static LucbirSearcher createCCVSearcher(){
        return new LucbirSearcher(new ColorCoherenceVector());
    }

    public static LucbirSearcher createHSVHSearcher(){
        return new LucbirSearcher(new HSVColorHistogram());
    }


    public static LucbirSearcher createPHashSearcher(){
        return new LucbirSearcher(new PHash());
    }

    public static LucbirSearcher createRGBHSearcher(){
        return new LucbirSearcher(new RGBColorHistogram());
    }

    public static LucbirSearcher createRHashSearcher(){
        return new LucbirSearcher(new RHash());
    }

    public static LucbirSearcher createULBPSearcher(){
        return new LucbirSearcher(new UniformLBP());
    }
}
