package org.microemu.app.capture;

/**
 * NeuQuant Neural-Net Quantization Algorithm
 *
 * Copyright (c) 1994 Anthony Dekker
 *
 * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994. See "Kohonen neural networks
 * for optimal colour quantization" in "Network: Computation in Neural Systems" Vol. 5 (1994) pp
 * 351-367. for a discussion of the algorithm.
 *
 * Any party obtaining a copy of these files from the author, directly or indirectly, is granted,
 * free of charge, a full and unrestricted irrevocable, world-wide, paid up, royalty-free,
 * nonexclusive right and license to deal in this software and documentation files (the "Software"),
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons who receive copies from any
 * such party to do so, with the only requirement being that this copyright notice remain intact.
 */

// Ported to Java 12/00 K Weiner

public class NeuQuant {

  protected static final int netsize = 256; /* number of colours used */

  /* four primes near 500 - assume no image has a length so large */
  /* that it is divisible by all four primes */
  protected static final int prime1 = 499;
  protected static final int prime2 = 491;
  protected static final int prime3 = 487;
  protected static final int prime4 = 503;

  protected static final int minpicturebytes = (3 * NeuQuant.prime4);
  /* minimum size for input image */

  /* Program Skeleton
     ----------------
     [select samplefac in range 1..30]
     [read image from input file]
     pic = (unsigned char*) malloc(3*width*height);
     initnet(pic,3*width*height,samplefac);
     learn();
     unbiasnet();
     [write output image header, using writecolourmap(f)]
     inxbuild();
     write output image using inxsearch(b,g,r)      */

  /* Network Definitions
     ------------------- */

  protected static final int maxnetpos = (NeuQuant.netsize - 1);
  protected static final int netbiasshift = 4; /* bias for colour values */
  protected static final int ncycles = 100; /* no. of learning cycles */

  /* defs for freq and bias */
  protected static final int intbiasshift = 16; /* bias for fractions */
  protected static final int intbias = ((1) << NeuQuant.intbiasshift);
  protected static final int gammashift = 10; /* gamma = 1024 */
  protected static final int gamma = ((1) << NeuQuant.gammashift);
  protected static final int betashift = 10;
  protected static final int beta = (NeuQuant.intbias >> NeuQuant.betashift); /* beta = 1/1024 */
  protected static final int betagamma = (NeuQuant.intbias << (NeuQuant.gammashift - NeuQuant.betashift));

  /* defs for decreasing radius factor */
  protected static final int initrad = (NeuQuant.netsize >> 3); /* for 256 cols, radius starts */
  protected static final int radiusbiasshift = 6; /* at 32.0 biased by 6 bits */
  protected static final int radiusbias = ((1) << NeuQuant.radiusbiasshift);
  protected static final int initradius = (NeuQuant.initrad * NeuQuant.radiusbias); /* and decreases by a */
  protected static final int radiusdec = 30; /* factor of 1/30 each cycle */

  /* defs for decreasing alpha factor */
  protected static final int alphabiasshift = 10; /* alpha starts at 1.0 */
  protected static final int initalpha = ((1) << NeuQuant.alphabiasshift);

  protected int alphadec; /* biased by 10 bits */

  /* radbias and alpharadbias used for radpower calculation */
  protected static final int radbiasshift = 8;
  protected static final int radbias = ((1) << NeuQuant.radbiasshift);
  protected static final int alpharadbshift = (NeuQuant.alphabiasshift + NeuQuant.radbiasshift);
  protected static final int alpharadbias = ((1) << NeuQuant.alpharadbshift);

  /* Types and Global Variables
  -------------------------- */

  protected byte[] thepicture; /* the input image itself */
  protected int lengthcount; /* lengthcount = H*W*3 */

  protected int samplefac; /* sampling factor 1..30 */

  //   typedef int pixel[4];                /* BGRc */
  protected int[][] network; /* the network itself - [netsize][4] */

  protected int[] netindex = new int[256];
  /* for network lookup - really 256 */

  protected int[] bias = new int[NeuQuant.netsize];
  /* bias and freq arrays for learning */
  protected int[] freq = new int[NeuQuant.netsize];
  protected int[] radpower = new int[NeuQuant.initrad];
  /* radpower for precomputation */

  /* Initialise network in range (0,0,0) to (255,255,255) and set parameters
     ----------------------------------------------------------------------- */
  public NeuQuant(final byte[] thepic, final int len, final int sample) {

    int i;
    int[] p;

    thepicture = thepic;
    lengthcount = len;
    samplefac = sample;

    network = new int[NeuQuant.netsize][];
    for (i = 0; i < NeuQuant.netsize; i++) {
      network[i] = new int[4];
      p = network[i];
      p[0] = p[1] = p[2] = (i << (NeuQuant.netbiasshift + 8)) / NeuQuant.netsize;
      freq[i] = NeuQuant.intbias / NeuQuant.netsize; /* 1/netsize */
      bias[i] = 0;
    }
  }

  public byte[] colorMap() {
    final byte[] map = new byte[3 * NeuQuant.netsize];
    final int[] index = new int[NeuQuant.netsize];
    for (int i = 0; i < NeuQuant.netsize; i++) {
      index[network[i][3]] = i;
    }
    int k = 0;
    for (int i = 0; i < NeuQuant.netsize; i++) {
      final int j = index[i];
      map[k++] = (byte)(network[j][0]);
      map[k++] = (byte)(network[j][1]);
      map[k++] = (byte)(network[j][2]);
    }
    return map;
  }

  /* Insertion sort of network and building of netindex[0..255] (to do after unbias)
     ------------------------------------------------------------------------------- */
  public void inxbuild() {

    int i, j, smallpos, smallval;
    int[] p;
    int[] q;
    int previouscol, startpos;

    previouscol = 0;
    startpos = 0;
    for (i = 0; i < NeuQuant.netsize; i++) {
      p = network[i];
      smallpos = i;
      smallval = p[1]; /* index on g */
      /* find smallest in i..netsize-1 */
      for (j = i + 1; j < NeuQuant.netsize; j++) {
        q = network[j];
        if (q[1] < smallval) { /* index on g */
          smallpos = j;
          smallval = q[1]; /* index on g */
        }
      }
      q = network[smallpos];
      /* swap p (i) and q (smallpos) entries */
      if (i != smallpos) {
        j = q[0];
        q[0] = p[0];
        p[0] = j;
        j = q[1];
        q[1] = p[1];
        p[1] = j;
        j = q[2];
        q[2] = p[2];
        p[2] = j;
        j = q[3];
        q[3] = p[3];
        p[3] = j;
      }
      /* smallval entry is now in position i */
      if (smallval != previouscol) {
        netindex[previouscol] = (startpos + i) >> 1;
        for (j = previouscol + 1; j < smallval; j++) {
          netindex[j] = i;
        }
        previouscol = smallval;
        startpos = i;
      }
    }
    netindex[previouscol] = (startpos + NeuQuant.maxnetpos) >> 1;
    for (j = previouscol + 1; j < 256; j++) {
      netindex[j] = NeuQuant.maxnetpos; /* really 256 */
    }
  }

  /* Main Learning Loop
     ------------------ */
  public void learn() {

    int i, j, b, g, r;
    int radius, rad, alpha, step, delta, samplepixels;
    byte[] p;
    int pix, lim;

    if (lengthcount < NeuQuant.minpicturebytes) {
      samplefac = 1;
    }
    alphadec = 30 + ((samplefac - 1) / 3);
    p = thepicture;
    pix = 0;
    lim = lengthcount;
    samplepixels = lengthcount / (3 * samplefac);
    delta = samplepixels / NeuQuant.ncycles;
    alpha = NeuQuant.initalpha;
    radius = NeuQuant.initradius;

    rad = radius >> NeuQuant.radiusbiasshift;
    if (rad <= 1) {
      rad = 0;
    }
    for (i = 0; i < rad; i++) {
      radpower[i] = alpha * ((((rad * rad) - (i * i)) * NeuQuant.radbias) / (rad * rad));
    }

    //fprintf(stderr,"beginning 1D learning: initial radius=%d\n", rad);

    if (lengthcount < NeuQuant.minpicturebytes) {
      step = 3;
    }
    else if ((lengthcount % NeuQuant.prime1) != 0) {
      step = 3 * NeuQuant.prime1;
    }
    else {
      if ((lengthcount % NeuQuant.prime2) != 0) {
        step = 3 * NeuQuant.prime2;
      }
      else {
        if ((lengthcount % NeuQuant.prime3) != 0) {
          step = 3 * NeuQuant.prime3;
        }
        else {
          step = 3 * NeuQuant.prime4;
        }
      }
    }

    i = 0;
    while (i < samplepixels) {
      b = (p[pix + 0] & 0xff) << NeuQuant.netbiasshift;
      g = (p[pix + 1] & 0xff) << NeuQuant.netbiasshift;
      r = (p[pix + 2] & 0xff) << NeuQuant.netbiasshift;
      j = contest(b, g, r);

      altersingle(alpha, j, b, g, r);
      if (rad != 0) {
        alterneigh(rad, j, b, g, r); /* alter neighbours */
      }

      pix += step;
      if (pix >= lim) {
        pix -= lengthcount;
      }

      i++;
      if (delta == 0) {
        delta = 1;
      }
      if ((i % delta) == 0) {
        alpha -= alpha / alphadec;
        radius -= radius / NeuQuant.radiusdec;
        rad = radius >> NeuQuant.radiusbiasshift;
        if (rad <= 1) {
          rad = 0;
        }
        for (j = 0; j < rad; j++) {
          radpower[j] = alpha * ((((rad * rad) - (j * j)) * NeuQuant.radbias) / (rad * rad));
        }
      }
    }
    //fprintf(stderr,"finished 1D learning: final alpha=%f !\n",((float)alpha)/initalpha);
  }

  /* Search for BGR values 0..255 (after net is unbiased) and return colour index
     ---------------------------------------------------------------------------- */
  public int map(final int b, final int g, final int r) {

    int i, j, dist, a, bestd;
    int[] p;
    int best;

    bestd = 1000; /* biggest possible dist is 256*3 */
    best = -1;
    i = netindex[g]; /* index on g */
    j = i - 1; /* start at netindex[g] and work outwards */

    while ((i < NeuQuant.netsize) || (j >= 0)) {
      if (i < NeuQuant.netsize) {
        p = network[i];
        dist = p[1] - g; /* inx key */
        if (dist >= bestd) {
          i = NeuQuant.netsize; /* stop iter */
        }
        else {
          i++;
          if (dist < 0) {
            dist = -dist;
          }
          a = p[0] - b;
          if (a < 0) {
            a = -a;
          }
          dist += a;
          if (dist < bestd) {
            a = p[2] - r;
            if (a < 0) {
              a = -a;
            }
            dist += a;
            if (dist < bestd) {
              bestd = dist;
              best = p[3];
            }
          }
        }
      }
      if (j >= 0) {
        p = network[j];
        dist = g - p[1]; /* inx key - reverse dif */
        if (dist >= bestd) {
          j = -1; /* stop iter */
        }
        else {
          j--;
          if (dist < 0) {
            dist = -dist;
          }
          a = p[0] - b;
          if (a < 0) {
            a = -a;
          }
          dist += a;
          if (dist < bestd) {
            a = p[2] - r;
            if (a < 0) {
              a = -a;
            }
            dist += a;
            if (dist < bestd) {
              bestd = dist;
              best = p[3];
            }
          }
        }
      }
    }
    return (best);
  }

  public byte[] process() {
    learn();
    unbiasnet();
    inxbuild();
    return colorMap();
  }

  /* Unbias network to give byte values 0..255 and record position i to prepare for sort
     ----------------------------------------------------------------------------------- */
  public void unbiasnet() {

    int i;
    for (i = 0; i < NeuQuant.netsize; i++) {
      network[i][0] >>= NeuQuant.netbiasshift;
      network[i][1] >>= NeuQuant.netbiasshift;
      network[i][2] >>= NeuQuant.netbiasshift;
      network[i][3] = i; /* record colour no */
    }
  }

  /* Move adjacent neurons by precomputed alpha*(1-((i-j)^2/[r]^2)) in radpower[|i-j|]
     --------------------------------------------------------------------------------- */
  protected void alterneigh(final int rad, final int i, final int b, final int g, final int r) {

    int j, k, lo, hi, a, m;
    int[] p;

    lo = i - rad;
    if (lo < -1) {
      lo = -1;
    }
    hi = i + rad;
    if (hi > NeuQuant.netsize) {
      hi = NeuQuant.netsize;
    }

    j = i + 1;
    k = i - 1;
    m = 1;
    while ((j < hi) || (k > lo)) {
      a = radpower[m++];
      if (j < hi) {
        p = network[j++];
        try {
          p[0] -= (a * (p[0] - b)) / NeuQuant.alpharadbias;
          p[1] -= (a * (p[1] - g)) / NeuQuant.alpharadbias;
          p[2] -= (a * (p[2] - r)) / NeuQuant.alpharadbias;
        }
        catch (final Exception e) {
        } // prevents 1.3 miscompilation
      }
      if (k > lo) {
        p = network[k--];
        try {
          p[0] -= (a * (p[0] - b)) / NeuQuant.alpharadbias;
          p[1] -= (a * (p[1] - g)) / NeuQuant.alpharadbias;
          p[2] -= (a * (p[2] - r)) / NeuQuant.alpharadbias;
        }
        catch (final Exception e) {
        }
      }
    }
  }

  /* Move neuron i towards biased (b,g,r) by factor alpha
     ---------------------------------------------------- */
  protected void altersingle(final int alpha, final int i, final int b, final int g, final int r) {

    /* alter hit neuron */
    final int[] n = network[i];
    n[0] -= (alpha * (n[0] - b)) / NeuQuant.initalpha;
    n[1] -= (alpha * (n[1] - g)) / NeuQuant.initalpha;
    n[2] -= (alpha * (n[2] - r)) / NeuQuant.initalpha;
  }

  /* Search for biased BGR values
     ---------------------------- */
  protected int contest(final int b, final int g, final int r) {

    /* finds closest neuron (min dist) and updates freq */
    /* finds best neuron (min dist-bias) and returns position */
    /* for frequently chosen neurons, freq[i] is high and bias[i] is negative */
    /* bias[i] = gamma*((1/netsize)-freq[i]) */

    int i, dist, a, biasdist, betafreq;
    int bestpos, bestbiaspos, bestd, bestbiasd;
    int[] n;

    bestd = ~((1) << 31);
    bestbiasd = bestd;
    bestpos = -1;
    bestbiaspos = bestpos;

    for (i = 0; i < NeuQuant.netsize; i++) {
      n = network[i];
      dist = n[0] - b;
      if (dist < 0) {
        dist = -dist;
      }
      a = n[1] - g;
      if (a < 0) {
        a = -a;
      }
      dist += a;
      a = n[2] - r;
      if (a < 0) {
        a = -a;
      }
      dist += a;
      if (dist < bestd) {
        bestd = dist;
        bestpos = i;
      }
      biasdist = dist - ((bias[i]) >> (NeuQuant.intbiasshift - NeuQuant.netbiasshift));
      if (biasdist < bestbiasd) {
        bestbiasd = biasdist;
        bestbiaspos = i;
      }
      betafreq = (freq[i] >> NeuQuant.betashift);
      freq[i] -= betafreq;
      bias[i] += (betafreq << NeuQuant.gammashift);
    }
    freq[bestpos] += NeuQuant.beta;
    bias[bestpos] -= NeuQuant.betagamma;
    return (bestbiaspos);
  }

}
