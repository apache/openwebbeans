/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.portable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.WebBeansContext;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version $Rev$ $Date$
 */
public class AnnotatedTypeImplTest
{
    @Test
    @Ignore("OWB-858 - AnnotatedTypeImpl not thread safe")
    public void test()
        throws Exception
    {

        final AnnotatedType<Colors> annotatedType =
            new AnnotatedTypeImpl<Colors>(new WebBeansContext(), Colors.class, null);

        int threads = 1000;
        final CountDownLatch startingLine = new CountDownLatch(threads);
        final CountDownLatch startingPistol = new CountDownLatch(1);
        final CountDownLatch finishLine = new CountDownLatch(threads);
        final AtomicInteger exceptions = new AtomicInteger();

        for (int i = 0; i < threads; i++)
        {
            new Runner(startingLine, startingPistol, exceptions, finishLine, annotatedType).start();
        }

        assertTrue("Not all threads reported ready.", startingLine.await(30, TimeUnit.SECONDS));

        startingPistol.countDown();

        assertTrue("Not all threads finished.", finishLine.await(30, TimeUnit.SECONDS));

        assertEquals(0, exceptions.get());
    }

    private static class Runner
        extends Thread
    {
        private final CountDownLatch startingLine;

        private final CountDownLatch startingPistol;

        private final AtomicInteger exceptions;

        private final CountDownLatch finishLine;

        private final AnnotatedType<Colors> annotatedType;

        public Runner(CountDownLatch startingLine, CountDownLatch startingPistol, AtomicInteger exceptions,
                      CountDownLatch finishLine, AnnotatedType<Colors> annotatedType)
        {
            this.startingLine = startingLine;
            this.startingPistol = startingPistol;
            this.exceptions = exceptions;
            this.finishLine = finishLine;
            this.annotatedType = annotatedType;

            setDaemon(true);
        }

        @Override
        public void run()
        {
            startingLine.countDown();
            try
            {
                startingPistol.await(10, TimeUnit.SECONDS);

                doit();

            }
            catch (Exception e)
            {
                e.printStackTrace();
                exceptions.incrementAndGet();
            }
            finally
            {
                finishLine.countDown();
            }
        }

        public void doit()
        {
            for (AnnotatedField<? super Colors> field : annotatedType.getFields())
            {
            }
        }
    }

    public static class Colors
    {
        private String almond;

        private String amber;

        private String amethyst;

        private String apple;

        private String apricot;

        private String aqua;

        private String aquamarine;

        private String ash;

        private String azure;

        private String banana;

        private String beige;

        private String black;

        private String blue;

        private String brick;

        private String bronze;

        private String brown;

        private String burgundy;

        private String carrot;

        private String charcoal;

        private String cherry;

        private String chestnut;

        private String chocolate;

        private String chrome;

        private String cinnamon;

        private String citrine;

        private String cobalt;

        private String copper;

        private String coral;

        private String cornflower;

        private String cotton;

        private String cream;

        private String crimson;

        private String cyan;

        private String ebony;

        private String emerald;

        private String forest;

        private String fuchsia;

        private String ginger;

        private String gold;

        private String goldenrod;

        private String gray;

        private String green;

        private String grey;

        private String indigo;

        private String ivory;

        private String jade;

        private String jasmine;

        private String khaki;

        private String lava;

        private String lavender;

        private String lemon;

        private String lilac;

        private String lime;

        private String macaroni;

        private String magenta;

        private String magnolia;

        private String mahogany;

        private String malachite;

        private String mango;

        private String maroon;

        private String mauve;

        private String mint;

        private String moonstone;

        private String navy;

        private String ocean;

        private String olive;

        private String onyx;

        private String orange;

        private String orchid;

        private String papaya;

        private String peach;

        private String pear;

        private String pearl;

        private String periwinkle;

        private String pine;

        private String pink;

        private String pistachio;

        private String platinum;

        private String plum;

        private String prune;

        private String pumpkin;

        private String purple;

        private String quartz;

        private String raspberry;

        private String red;

        private String rose;

        private String rosewood;

        private String ruby;

        private String salmon;

        private String sapphire;

        private String scarlet;

        private String sienna;

        private String silver;

        private String slate;

        private String strawberry;

        private String tan;

        private String tangerine;

        private String taupe;

        private String teal;

        private String titanium;

        private String topaz;

        private String turquoise;

        private String umber;

        private String vanilla;

        private String violet;

        private String watermelon;

        private String white;

        private String yellow;

        public String getAlmond()
        {
            return almond;
        }

        public void setAlmond(String almond)
        {
            this.almond = almond;
        }

        public String getAmber()
        {
            return amber;
        }

        public void setAmber(String amber)
        {
            this.amber = amber;
        }

        public String getAmethyst()
        {
            return amethyst;
        }

        public void setAmethyst(String amethyst)
        {
            this.amethyst = amethyst;
        }

        public String getApple()
        {
            return apple;
        }

        public void setApple(String apple)
        {
            this.apple = apple;
        }

        public String getApricot()
        {
            return apricot;
        }

        public void setApricot(String apricot)
        {
            this.apricot = apricot;
        }

        public String getAqua()
        {
            return aqua;
        }

        public void setAqua(String aqua)
        {
            this.aqua = aqua;
        }

        public String getAquamarine()
        {
            return aquamarine;
        }

        public void setAquamarine(String aquamarine)
        {
            this.aquamarine = aquamarine;
        }

        public String getAsh()
        {
            return ash;
        }

        public void setAsh(String ash)
        {
            this.ash = ash;
        }

        public String getAzure()
        {
            return azure;
        }

        public void setAzure(String azure)
        {
            this.azure = azure;
        }

        public String getBanana()
        {
            return banana;
        }

        public void setBanana(String banana)
        {
            this.banana = banana;
        }

        public String getBeige()
        {
            return beige;
        }

        public void setBeige(String beige)
        {
            this.beige = beige;
        }

        public String getBlack()
        {
            return black;
        }

        public void setBlack(String black)
        {
            this.black = black;
        }

        public String getBlue()
        {
            return blue;
        }

        public void setBlue(String blue)
        {
            this.blue = blue;
        }

        public String getBrick()
        {
            return brick;
        }

        public void setBrick(String brick)
        {
            this.brick = brick;
        }

        public String getBronze()
        {
            return bronze;
        }

        public void setBronze(String bronze)
        {
            this.bronze = bronze;
        }

        public String getBrown()
        {
            return brown;
        }

        public void setBrown(String brown)
        {
            this.brown = brown;
        }

        public String getBurgundy()
        {
            return burgundy;
        }

        public void setBurgundy(String burgundy)
        {
            this.burgundy = burgundy;
        }

        public String getCarrot()
        {
            return carrot;
        }

        public void setCarrot(String carrot)
        {
            this.carrot = carrot;
        }

        public String getCharcoal()
        {
            return charcoal;
        }

        public void setCharcoal(String charcoal)
        {
            this.charcoal = charcoal;
        }

        public String getCherry()
        {
            return cherry;
        }

        public void setCherry(String cherry)
        {
            this.cherry = cherry;
        }

        public String getChestnut()
        {
            return chestnut;
        }

        public void setChestnut(String chestnut)
        {
            this.chestnut = chestnut;
        }

        public String getChocolate()
        {
            return chocolate;
        }

        public void setChocolate(String chocolate)
        {
            this.chocolate = chocolate;
        }

        public String getChrome()
        {
            return chrome;
        }

        public void setChrome(String chrome)
        {
            this.chrome = chrome;
        }

        public String getCinnamon()
        {
            return cinnamon;
        }

        public void setCinnamon(String cinnamon)
        {
            this.cinnamon = cinnamon;
        }

        public String getCitrine()
        {
            return citrine;
        }

        public void setCitrine(String citrine)
        {
            this.citrine = citrine;
        }

        public String getCobalt()
        {
            return cobalt;
        }

        public void setCobalt(String cobalt)
        {
            this.cobalt = cobalt;
        }

        public String getCopper()
        {
            return copper;
        }

        public void setCopper(String copper)
        {
            this.copper = copper;
        }

        public String getCoral()
        {
            return coral;
        }

        public void setCoral(String coral)
        {
            this.coral = coral;
        }

        public String getCornflower()
        {
            return cornflower;
        }

        public void setCornflower(String cornflower)
        {
            this.cornflower = cornflower;
        }

        public String getCotton()
        {
            return cotton;
        }

        public void setCotton(String cotton)
        {
            this.cotton = cotton;
        }

        public String getCream()
        {
            return cream;
        }

        public void setCream(String cream)
        {
            this.cream = cream;
        }

        public String getCrimson()
        {
            return crimson;
        }

        public void setCrimson(String crimson)
        {
            this.crimson = crimson;
        }

        public String getCyan()
        {
            return cyan;
        }

        public void setCyan(String cyan)
        {
            this.cyan = cyan;
        }

        public String getEbony()
        {
            return ebony;
        }

        public void setEbony(String ebony)
        {
            this.ebony = ebony;
        }

        public String getEmerald()
        {
            return emerald;
        }

        public void setEmerald(String emerald)
        {
            this.emerald = emerald;
        }

        public String getForest()
        {
            return forest;
        }

        public void setForest(String forest)
        {
            this.forest = forest;
        }

        public String getFuchsia()
        {
            return fuchsia;
        }

        public void setFuchsia(String fuchsia)
        {
            this.fuchsia = fuchsia;
        }

        public String getGinger()
        {
            return ginger;
        }

        public void setGinger(String ginger)
        {
            this.ginger = ginger;
        }

        public String getGold()
        {
            return gold;
        }

        public void setGold(String gold)
        {
            this.gold = gold;
        }

        public String getGoldenrod()
        {
            return goldenrod;
        }

        public void setGoldenrod(String goldenrod)
        {
            this.goldenrod = goldenrod;
        }

        public String getGray()
        {
            return gray;
        }

        public void setGray(String gray)
        {
            this.gray = gray;
        }

        public String getGreen()
        {
            return green;
        }

        public void setGreen(String green)
        {
            this.green = green;
        }

        public String getGrey()
        {
            return grey;
        }

        public void setGrey(String grey)
        {
            this.grey = grey;
        }

        public String getIndigo()
        {
            return indigo;
        }

        public void setIndigo(String indigo)
        {
            this.indigo = indigo;
        }

        public String getIvory()
        {
            return ivory;
        }

        public void setIvory(String ivory)
        {
            this.ivory = ivory;
        }

        public String getJade()
        {
            return jade;
        }

        public void setJade(String jade)
        {
            this.jade = jade;
        }

        public String getJasmine()
        {
            return jasmine;
        }

        public void setJasmine(String jasmine)
        {
            this.jasmine = jasmine;
        }

        public String getKhaki()
        {
            return khaki;
        }

        public void setKhaki(String khaki)
        {
            this.khaki = khaki;
        }

        public String getLava()
        {
            return lava;
        }

        public void setLava(String lava)
        {
            this.lava = lava;
        }

        public String getLavender()
        {
            return lavender;
        }

        public void setLavender(String lavender)
        {
            this.lavender = lavender;
        }

        public String getLemon()
        {
            return lemon;
        }

        public void setLemon(String lemon)
        {
            this.lemon = lemon;
        }

        public String getLilac()
        {
            return lilac;
        }

        public void setLilac(String lilac)
        {
            this.lilac = lilac;
        }

        public String getLime()
        {
            return lime;
        }

        public void setLime(String lime)
        {
            this.lime = lime;
        }

        public String getMacaroni()
        {
            return macaroni;
        }

        public void setMacaroni(String macaroni)
        {
            this.macaroni = macaroni;
        }

        public String getMagenta()
        {
            return magenta;
        }

        public void setMagenta(String magenta)
        {
            this.magenta = magenta;
        }

        public String getMagnolia()
        {
            return magnolia;
        }

        public void setMagnolia(String magnolia)
        {
            this.magnolia = magnolia;
        }

        public String getMahogany()
        {
            return mahogany;
        }

        public void setMahogany(String mahogany)
        {
            this.mahogany = mahogany;
        }

        public String getMalachite()
        {
            return malachite;
        }

        public void setMalachite(String malachite)
        {
            this.malachite = malachite;
        }

        public String getMango()
        {
            return mango;
        }

        public void setMango(String mango)
        {
            this.mango = mango;
        }

        public String getMaroon()
        {
            return maroon;
        }

        public void setMaroon(String maroon)
        {
            this.maroon = maroon;
        }

        public String getMauve()
        {
            return mauve;
        }

        public void setMauve(String mauve)
        {
            this.mauve = mauve;
        }

        public String getMint()
        {
            return mint;
        }

        public void setMint(String mint)
        {
            this.mint = mint;
        }

        public String getMoonstone()
        {
            return moonstone;
        }

        public void setMoonstone(String moonstone)
        {
            this.moonstone = moonstone;
        }

        public String getNavy()
        {
            return navy;
        }

        public void setNavy(String navy)
        {
            this.navy = navy;
        }

        public String getOcean()
        {
            return ocean;
        }

        public void setOcean(String ocean)
        {
            this.ocean = ocean;
        }

        public String getOlive()
        {
            return olive;
        }

        public void setOlive(String olive)
        {
            this.olive = olive;
        }

        public String getOnyx()
        {
            return onyx;
        }

        public void setOnyx(String onyx)
        {
            this.onyx = onyx;
        }

        public String getOrange()
        {
            return orange;
        }

        public void setOrange(String orange)
        {
            this.orange = orange;
        }

        public String getOrchid()
        {
            return orchid;
        }

        public void setOrchid(String orchid)
        {
            this.orchid = orchid;
        }

        public String getPapaya()
        {
            return papaya;
        }

        public void setPapaya(String papaya)
        {
            this.papaya = papaya;
        }

        public String getPeach()
        {
            return peach;
        }

        public void setPeach(String peach)
        {
            this.peach = peach;
        }

        public String getPear()
        {
            return pear;
        }

        public void setPear(String pear)
        {
            this.pear = pear;
        }

        public String getPearl()
        {
            return pearl;
        }

        public void setPearl(String pearl)
        {
            this.pearl = pearl;
        }

        public String getPeriwinkle()
        {
            return periwinkle;
        }

        public void setPeriwinkle(String periwinkle)
        {
            this.periwinkle = periwinkle;
        }

        public String getPine()
        {
            return pine;
        }

        public void setPine(String pine)
        {
            this.pine = pine;
        }

        public String getPink()
        {
            return pink;
        }

        public void setPink(String pink)
        {
            this.pink = pink;
        }

        public String getPistachio()
        {
            return pistachio;
        }

        public void setPistachio(String pistachio)
        {
            this.pistachio = pistachio;
        }

        public String getPlatinum()
        {
            return platinum;
        }

        public void setPlatinum(String platinum)
        {
            this.platinum = platinum;
        }

        public String getPlum()
        {
            return plum;
        }

        public void setPlum(String plum)
        {
            this.plum = plum;
        }

        public String getPrune()
        {
            return prune;
        }

        public void setPrune(String prune)
        {
            this.prune = prune;
        }

        public String getPumpkin()
        {
            return pumpkin;
        }

        public void setPumpkin(String pumpkin)
        {
            this.pumpkin = pumpkin;
        }

        public String getPurple()
        {
            return purple;
        }

        public void setPurple(String purple)
        {
            this.purple = purple;
        }

        public String getQuartz()
        {
            return quartz;
        }

        public void setQuartz(String quartz)
        {
            this.quartz = quartz;
        }

        public String getRaspberry()
        {
            return raspberry;
        }

        public void setRaspberry(String raspberry)
        {
            this.raspberry = raspberry;
        }

        public String getRed()
        {
            return red;
        }

        public void setRed(String red)
        {
            this.red = red;
        }

        public String getRose()
        {
            return rose;
        }

        public void setRose(String rose)
        {
            this.rose = rose;
        }

        public String getRosewood()
        {
            return rosewood;
        }

        public void setRosewood(String rosewood)
        {
            this.rosewood = rosewood;
        }

        public String getRuby()
        {
            return ruby;
        }

        public void setRuby(String ruby)
        {
            this.ruby = ruby;
        }

        public String getSalmon()
        {
            return salmon;
        }

        public void setSalmon(String salmon)
        {
            this.salmon = salmon;
        }

        public String getSapphire()
        {
            return sapphire;
        }

        public void setSapphire(String sapphire)
        {
            this.sapphire = sapphire;
        }

        public String getScarlet()
        {
            return scarlet;
        }

        public void setScarlet(String scarlet)
        {
            this.scarlet = scarlet;
        }

        public String getSienna()
        {
            return sienna;
        }

        public void setSienna(String sienna)
        {
            this.sienna = sienna;
        }

        public String getSilver()
        {
            return silver;
        }

        public void setSilver(String silver)
        {
            this.silver = silver;
        }

        public String getSlate()
        {
            return slate;
        }

        public void setSlate(String slate)
        {
            this.slate = slate;
        }

        public String getStrawberry()
        {
            return strawberry;
        }

        public void setStrawberry(String strawberry)
        {
            this.strawberry = strawberry;
        }

        public String getTan()
        {
            return tan;
        }

        public void setTan(String tan)
        {
            this.tan = tan;
        }

        public String getTangerine()
        {
            return tangerine;
        }

        public void setTangerine(String tangerine)
        {
            this.tangerine = tangerine;
        }

        public String getTaupe()
        {
            return taupe;
        }

        public void setTaupe(String taupe)
        {
            this.taupe = taupe;
        }

        public String getTeal()
        {
            return teal;
        }

        public void setTeal(String teal)
        {
            this.teal = teal;
        }

        public String getTitanium()
        {
            return titanium;
        }

        public void setTitanium(String titanium)
        {
            this.titanium = titanium;
        }

        public String getTopaz()
        {
            return topaz;
        }

        public void setTopaz(String topaz)
        {
            this.topaz = topaz;
        }

        public String getTurquoise()
        {
            return turquoise;
        }

        public void setTurquoise(String turquoise)
        {
            this.turquoise = turquoise;
        }

        public String getUmber()
        {
            return umber;
        }

        public void setUmber(String umber)
        {
            this.umber = umber;
        }

        public String getVanilla()
        {
            return vanilla;
        }

        public void setVanilla(String vanilla)
        {
            this.vanilla = vanilla;
        }

        public String getViolet()
        {
            return violet;
        }

        public void setViolet(String violet)
        {
            this.violet = violet;
        }

        public String getWatermelon()
        {
            return watermelon;
        }

        public void setWatermelon(String watermelon)
        {
            this.watermelon = watermelon;
        }

        public String getWhite()
        {
            return white;
        }

        public void setWhite(String white)
        {
            this.white = white;
        }

        public String getYellow()
        {
            return yellow;
        }

        public void setYellow(String yellow)
        {
            this.yellow = yellow;
        }
    }
}


