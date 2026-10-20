#version 150

uniform vec4 ColorModulator;
uniform float Time;
uniform float Intensity;
uniform float Speed;
uniform vec3 ThemeColor;
uniform float Scale;
uniform float AuroraHeight;
uniform float CurtainWidth;
uniform float CurtainCount;
uniform float StarStrength;
uniform float SkyTint;
uniform float Opacity;
uniform float Style;
uniform vec2 ProjectionScale;
uniform vec3 CameraForward;
uniform vec3 CameraRight;
uniform vec3 CameraUp;
uniform float FarDepth;

smooth in vec2 ScreenPosition;
smooth in vec4 FragColor;

out vec4 fragColor;

vec3 interpolatedSkyDirection() {
    vec2 safeProjectionScale = max(abs(ProjectionScale), vec2(0.0001));
    vec2 viewPlane = ScreenPosition / safeProjectionScale;
    return normalize(CameraForward + CameraRight * viewPlane.x + CameraUp * viewPlane.y);
}

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += noise(p) * amplitude;
        p *= 2.05;
        amplitude *= 0.52;
    }
    return value * 1.07;
}

float star(vec2 uv) {
    vec2 grid = uv * vec2(210.0, 120.0);
    vec2 cell = floor(grid);
    vec2 local = fract(grid) - 0.5;
    float rnd = hash(cell);
    float sparkle = smoothstep(0.996, 1.0, rnd);
    float shape = exp(-dot(local, local) * 120.0);
    return sparkle * shape;
}

// Adapted from "Chill Smoke Orb" by diatribes:
// https://www.shadertoy.com/view/tflBDM
float chillOrbDistance(vec3 p, float time) {
    float orbTime = time * 4.0;
    vec3 center = vec3(
        sin(sin(orbTime * 0.2) + orbTime * 0.4) * 6.0,
        1.0 + sin(sin(orbTime * 0.5) + orbTime * 0.2) * 4.0,
        12.0 + time + cos(orbTime * 0.3) * 8.0
    );
    return length(p - center);
}

float chillSmokeOrb(vec3 rayDirection, float time) {
    float distanceTravelled = 0.0;
    float glow = 0.0;

    for (int i = 0; i < 32; i++) {
        vec3 p = rayDirection * distanceTravelled + vec3(0.0, 0.0, time);
        float orbDistance = max(0.5 * (chillOrbDistance(p, time) - 0.1), 0.01);

        p.xy *= mat2(cos(0.1 * time + p.z / 8.0 + vec4(0.0, 33.0, 11.0, 0.0)));
        float smokeDistance = 4.0 - abs(p.y);
        for (float octave = 0.8; octave < 8.0; octave += octave) {
            p += cos(0.7 * time + p.yzx) * 0.2;
            smokeDistance -= abs(dot(sin(0.1 * time + p * octave), 0.6 + p - p)) / octave;
        }

        float stepDistance = min(0.03 + 0.2 * abs(smokeDistance), orbDistance);
        distanceTravelled += stepDistance;
        glow += 1.0 / (stepDistance + orbDistance * 3.0);
    }

    return tanh(glow / 2.5);
}

// Adapted from "Smoke noise" by And390:
// https://www.shadertoy.com/view/MsdGWn
// Its textureless simplex implementation is distributed under the MIT License.
vec3 smokeMod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 smokeMod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 smokePermute(vec4 x) {
    return smokeMod289(((x * 34.0) + 1.0) * x);
}

vec4 smokeTaylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float smokeSimplex(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;

    i = smokeMod289(i);
    vec4 p = smokePermute(smokePermute(smokePermute(
        i.z + vec4(0.0, i1.z, i2.z, 1.0))
        + i.y + vec4(0.0, i1.y, i2.y, 1.0))
        + i.x + vec4(0.0, i1.x, i2.x, 1.0));

    float n = 1.0 / 7.0;
    vec3 ns = n * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 xFloor = floor(j * ns.z);
    vec4 yFloor = floor(j - 7.0 * xFloor);
    vec4 x = xFloor * ns.x + ns.yyyy;
    vec4 y = yFloor * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = smokeTaylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m *= m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float smokeFbm3(vec3 v) {
    float result = smokeSimplex(v);
    result += smokeSimplex(v * 2.0) / 2.0;
    result += smokeSimplex(v * 4.0) / 4.0;
    return result / 1.75;
}

float smokeFbm5(vec3 v) {
    float result = smokeSimplex(v);
    result += smokeSimplex(v * 2.0) / 2.0;
    result += smokeSimplex(v * 4.0) / 4.0;
    result += smokeSimplex(v * 8.0) / 8.0;
    return result / 1.875;
}

float warpedSmokeNoise(vec3 v) {
    const float warpStep = 0.5;
    for (int i = 0; i < 1; i++) {
        v.xy += vec2(smokeFbm3(v), smokeFbm3(vec3(v.xy, v.z + 1000.0))) * warpStep;
    }
    return smokeFbm5(v) * 0.5 + 0.5;
}

// Adapted from "Tileable Water Caustic" by David Hoskins and joltz0r:
// https://www.shadertoy.com/view/MdlXz8
float waterCaustics(vec2 coordinate, float time) {
    const float tau = 6.28318530718;
    vec2 p = coordinate * tau - 250.0;
    vec2 iteration = p;
    float caustic = 1.0;
    const float detail = 0.005;

    for (int layer = 0; layer < 4; layer++) {
        float layerTime = (time * 0.5 + 23.0) * (1.0 - 3.5 / float(layer + 1));
        iteration = p + vec2(
            cos(layerTime - iteration.x) + sin(layerTime + iteration.y),
            sin(layerTime - iteration.y) + cos(layerTime + iteration.x)
        );
        vec2 divisor = vec2(sin(iteration.x + layerTime), cos(iteration.y + layerTime)) / detail;
        divisor = sign(divisor) * max(abs(divisor), vec2(0.0001));
        caustic += 1.0 / length(p / divisor);
    }

    caustic = 1.17 - pow(caustic / 5.0, 1.4);
    return clamp(pow(abs(caustic), 8.0), 0.0, 1.0);
}

// Adapted from "Worley Noise Waters" by Kyle273:
// https://www.shadertoy.com/view/llS3RK
float worleyRandom(vec2 p) {
    return hash(p * vec2(0.91, 1.17) + 4.73);
}

float worleyLayer(vec2 p) {
    float distanceSquared = 1e30;
    for (int xOffset = -1; xOffset <= 1; xOffset++) {
        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            vec2 point = floor(p) + vec2(float(xOffset), float(yOffset));
            vec2 difference = p - point - worleyRandom(point);
            distanceSquared = min(distanceSquared, dot(difference, difference));
        }
    }
    return 3.0 * exp(-4.0 * abs(2.5 * distanceSquared - 1.0));
}

float layeredWorley(vec2 p, float time) {
    float large = worleyLayer(p * 5.0 + 0.05 * time);
    float fine = sqrt(worleyLayer(p * 50.0 + 0.12 - 0.1 * time));
    float reverse = sqrt(sqrt(worleyLayer(p * -10.0 + 0.03 * time)));
    return sqrt(sqrt(sqrt(max(large * fine * reverse, 0.0))));
}

// Adapted from "Perlin Water Noise" by jackdavenport:
// https://www.shadertoy.com/view/Mt2SzR
float perlinWaterRandom(float x) {
    return hash(vec2(x, x * 0.6180339 + 7.13));
}

float perlinWaterNoise(vec2 p) {
    return perlinWaterRandom(p.x + p.y * 10000.0);
}

float smoothPerlinWater(vec2 p) {
    vec2 interpolation = smoothstep(0.0, 1.0, fract(p));
    vec2 southWest = floor(p);
    vec2 southEast = vec2(ceil(p.x), floor(p.y));
    vec2 northWest = vec2(floor(p.x), ceil(p.y));
    vec2 northEast = ceil(p);
    float south = mix(perlinWaterNoise(southWest), perlinWaterNoise(southEast), interpolation.x);
    float north = mix(perlinWaterNoise(northWest), perlinWaterNoise(northEast), interpolation.x);
    return mix(south, north, interpolation.y);
}

float fractalPerlinWater(vec2 p) {
    float value = smoothPerlinWater(p);
    value += smoothPerlinWater(p * 2.0) / 2.0;
    value += smoothPerlinWater(p * 4.0) / 4.0;
    return value / 1.75;
}

float movingPerlinWater(vec2 p, float time) {
    float x = fractalPerlinWater(p + time);
    float y = fractalPerlinWater(p - time);
    return fractalPerlinWater(p + vec2(x, y));
}

float nestedPerlinWater(vec2 p, float time) {
    float x = movingPerlinWater(p, time);
    float y = movingPerlinWater(p + 100.0, time);
    return movingPerlinWater(p + vec2(x, y), time);
}

void main() {
    vec3 direction = interpolatedSkyDirection();
    vec2 sphereCoord = vec2(
        atan(direction.z, direction.x) / 6.28318530718 + 0.5,
        asin(clamp(direction.y, -1.0, 1.0)) / 3.14159265359 + 0.5
    );
    float angle = sphereCoord.x * 6.28318530718;
    vec2 circular = vec2(cos(angle), sin(angle));
    vec2 seamlessCoordinate = vec2(
        direction.x + direction.y * 0.35,
        direction.z - direction.y * 0.25
    );

    float t = Time * Speed;
    float horizon = smoothstep(0.0, 0.85, sphereCoord.y);

    vec3 theme = max(ThemeColor, vec3(0.015));
    vec3 deepNight = mix(vec3(0.004, 0.008, 0.025), theme * 0.055, SkyTint * 0.55);
    vec3 upperSky = mix(vec3(0.015, 0.030, 0.075), theme * 0.16, SkyTint * 0.62);
    vec3 sky = mix(deepNight, upperSky, horizon);
    sky += mix(vec3(0.012, 0.010, 0.025), theme * 0.09, SkyTint * 0.45) * pow(max(sphereCoord.y, 0.0), 3.0);
    vec3 baseSky = sky;

    if (Style < 0.5) {
        float curtain = 0.0;
        for (int i = 0; i < 6; i++) {
            float fi = float(i);
            if (fi >= CurtainCount) {
                continue;
            }
            float y = AuroraHeight + fi * 0.095;
            vec2 noisePosition = circular * (1.7 + fi * 0.22) * Scale + vec2(fi * 2.7 + t * 0.08, t * 0.12);
            float wave = sin(angle * (2.0 + fi) * Scale + t * (0.65 + fi * 0.12) + fbm(noisePosition) * 2.8);
            float bandSharpness = (14.0 + fi * 2.2) / max(CurtainWidth, 0.2);
            float band = exp(-pow((sphereCoord.y - y - wave * 0.045) * bandSharpness, 2.0));
            float verticalFade = smoothstep(0.12, 0.58, sphereCoord.y) * (1.0 - smoothstep(0.92, 1.0, sphereCoord.y));
            curtain += band * verticalFade * (0.75 - fi * 0.09);
        }

        curtain = 1.0 - exp(-curtain * 0.92);
        float flowingDetail = fbm(circular * 4.0 + vec2(t * 0.08, t * 0.22));
        curtain *= mix(0.78, 1.12, flowingDetail);
        vec3 auroraLow = mix(theme * 0.72, theme, 0.55);
        vec3 auroraHigh = mix(theme, vec3(1.0), 0.42);
        vec3 shiftedTheme = mix(theme, theme.brg, 0.18);
        vec3 aurora = mix(auroraLow, auroraHigh, smoothstep(0.25, 0.85, sphereCoord.y));
        aurora = mix(aurora, shiftedTheme, smoothstep(0.55, 0.95, fbm(circular * 1.1 + vec2(t * 0.03, t * 0.08))));
        sky += aurora * curtain * Intensity;
    } else if (Style < 1.5) {
        vec2 nebulaPosition = circular * 2.8 * Scale + vec2(direction.y * 2.2 + t * 0.07, t * 0.05);
        float cloudLarge = fbm(nebulaPosition);
        float cloudDetail = fbm(circular.yx * 6.0 * Scale + vec2(t * -0.04, direction.y * 4.0));
        float cloud = smoothstep(0.38, 0.92, cloudLarge * 0.78 + cloudDetail * 0.48);
        float cloudFade = smoothstep(0.08, AuroraHeight + 0.18, sphereCoord.y) * (1.0 - smoothstep(0.88, 1.0, sphereCoord.y));
        vec3 nebulaColor = mix(theme * 0.32, mix(theme, vec3(1.0), 0.62), cloudDetail);
        sky += nebulaColor * cloud * cloudFade * Intensity * CurtainWidth;
    } else if (Style < 2.5) {
        float tiltedLatitude = direction.y * 0.82 + direction.x * 0.36 - direction.z * 0.18;
        float galaxyBand = exp(-pow(tiltedLatitude * 7.0 / max(CurtainWidth, 0.1), 2.0));
        float galaxyDust = fbm(circular * 6.0 * Scale + vec2(direction.y * 7.0, t * 0.035));
        float darkLane = fbm(circular.yx * 11.0 * Scale + vec2(t * -0.025, direction.y * 9.0));
        float galaxyLight = galaxyBand * smoothstep(0.22, 0.9, galaxyDust) * (0.45 + darkLane * 0.75);
        vec3 galaxyColor = mix(theme * 0.38, mix(theme, vec3(1.0), 0.72), galaxyDust);
        sky += galaxyColor * galaxyLight * Intensity * 1.35;
    } else if (Style < 3.5) {
        float vortexRadius = clamp(1.0 - (sphereCoord.y - AuroraHeight) / max(1.0 - AuroraHeight, 0.1), 0.0, 1.0);
        float spiral = sin(angle * 5.0 + vortexRadius * 28.0 * Scale - t * 1.8 + fbm(circular * 3.0 + vec2(t * 0.05)) * 3.0);
        float filament = pow(max(0.0, spiral), 5.0 / max(CurtainWidth, 0.2));
        float vortexFade = smoothstep(AuroraHeight - 0.08, AuroraHeight + 0.18, sphereCoord.y);
        float core = exp(-vortexRadius * 8.0);
        vec3 vortexColor = mix(theme, vec3(1.0), clamp(filament * 0.5 + core, 0.0, 1.0));
        sky += vortexColor * (filament * 0.72 + core * 1.4) * vortexFade * Intensity;
    } else if (Style < 4.5) {
        float smokeNoise = fbm(circular * 4.0 * Scale + vec2(direction.y * 3.0 + t * 0.04, t * 0.09));
        float smoke = 0.0;
        float smokeCore = 0.0;
        for (int i = 0; i < 10; i++) {
            float fi = float(i);
            float randomAngle = hash(vec2(fi * 1.73, 4.21)) * 6.28318530718;
            float rise = fract(hash(vec2(fi * 2.37, 8.13)) + t * (0.018 + hash(vec2(fi, 1.0)) * 0.018));
            float latitude = mix(-0.42, 1.30, rise);
            float horizontal = cos(latitude);
            vec3 bubbleDirection = normalize(vec3(
                cos(randomAngle + t * (0.025 + fi * 0.0015)) * horizontal,
                sin(latitude),
                sin(randomAngle + t * (0.025 + fi * 0.0015)) * horizontal
            ));
            float distanceToBubble = acos(clamp(dot(direction, bubbleDirection), -1.0, 1.0));
            distanceToBubble += (smokeNoise - 0.5) * 0.035 * CurtainWidth;
            float radius = (0.055 + hash(vec2(fi * 3.11, 2.19)) * 0.095) / max(Scale, 0.25);
            float shellWidth = radius * (0.16 + 0.09 * CurtainWidth);
            float shell = exp(-pow((distanceToBubble - radius) / max(shellWidth, 0.008), 2.0));
            float coreBubble = exp(-pow(distanceToBubble / max(radius * 1.15, 0.01), 2.0));
            float lifeFade = smoothstep(0.0, 0.12, rise) * (1.0 - smoothstep(0.82, 1.0, rise));
            smoke += shell * lifeFade;
            smokeCore += coreBubble * lifeFade;
        }
        vec3 smokeColor = mix(theme * 0.52, mix(theme, vec3(1.0), 0.68), smokeNoise);
        float smokeAmount = smoke * 0.45 + smokeCore * 0.18;
        sky += smokeColor * smokeAmount * Intensity * (0.65 + CurtainWidth * 0.35);
    } else if (Style < 5.5) {
        float orbSmoke = chillSmokeOrb(direction, t);
        vec3 orbSmokeColor = mix(theme * 0.3, mix(theme, vec3(1.0), 0.78), orbSmoke);
        sky += orbSmokeColor * orbSmoke * Intensity;
    } else if (Style < 6.5) {
        vec3 smokePosition = direction * (2.0 * Scale) + vec3(0.0, 0.0, t * 0.1);
        float smokeValue = clamp(warpedSmokeNoise(smokePosition), 0.0, 1.0);
        smokeValue = smokeValue * smokeValue * smokeValue * smokeValue * 2.0;
        vec3 smokeNoiseColor = mix(theme * 0.22, mix(theme, vec3(1.0), 0.7), clamp(smokeValue, 0.0, 1.0));
        sky += smokeNoiseColor * smokeValue * Intensity * (0.65 + CurtainWidth * 0.35);
    } else if (Style < 7.5) {
        float caustic = waterCaustics(seamlessCoordinate * Scale, t);
        vec3 causticColor = mix(theme * 0.22, mix(theme, vec3(1.0), 0.78), caustic);
        sky = mix(sky, causticColor, clamp(Intensity * 0.72, 0.0, 1.0));
    } else if (Style < 8.5) {
        float worley = clamp(layeredWorley(seamlessCoordinate * 0.35 * Scale, t), 0.0, 1.0);
        worley *= 0.68 + 0.32 * horizon;
        vec3 worleyColor = mix(theme * 0.16, mix(theme, vec3(1.0), 0.72), worley) * worley;
        sky = mix(sky, worleyColor, clamp(Intensity * 0.78, 0.0, 1.0));
    } else {
        float perlinWater = nestedPerlinWater(seamlessCoordinate * 6.0 * Scale, t);
        vec3 perlinColor = mix(mix(theme, vec3(1.0), 0.58), theme * 0.25, perlinWater);
        sky = mix(sky, perlinColor, clamp(Intensity * 0.74, 0.0, 1.0));
    }
    sky += vec3(1.0, 0.95, 0.82) * star(sphereCoord) * (0.45 + 0.55 * horizon) * StarStrength;

    float vignette = smoothstep(1.25, 0.25, abs(sphereCoord.y - 0.5));
    sky *= 0.72 + vignette * 0.28;
    baseSky *= 0.72 + vignette * 0.28;

    // Keep server-pack sky textures from bleeding through the procedural result.
    vec3 finalSky = mix(baseSky, sky, clamp(Opacity, 0.0, 1.0));
    fragColor = vec4(finalSky * ColorModulator.rgb * FragColor.rgb, 1.0);
}
