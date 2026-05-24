package com.darkona.droplets.content;

import com.darkona.droplets.api.spi.DropletsService;

/**
 * What {@link com.darkona.droplets.api.DropletsAPI} calls; set in the mod constructor.
 */
public final class DropletsServiceImpl implements DropletsService
{
    public static final DropletsServiceImpl INSTANCE = new DropletsServiceImpl();

    private DropletsServiceImpl() {}
}
