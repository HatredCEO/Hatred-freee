float roundedBoxSDF(vec2 centerPosition, vec2 size, vec4 radius) {
    vec2 halfSize = size;
    radius = min(radius, vec4(halfSize.x, halfSize.y, halfSize.x, halfSize.y));

    radius.xy = (centerPosition.x > 0.0) ? radius.xy : radius.zw;
    radius.x = (centerPosition.y > 0.0) ? radius.x : radius.y;

    vec2 q = abs(centerPosition) - size + radius.x;
    vec2 d = max(q, 0.0);
    
    // Squircle / Superellipse curvature exponent (p = 3.2 for rounder continuous smooth corners)
    float p = 3.2;
    float squircleDist = (d.x > 0.0 && d.y > 0.0) ? pow(pow(d.x, p) + pow(d.y, p), 1.0 / p) : (d.x + d.y);
    return min(max(q.x, q.y), 0.0) + squircleDist - radius.x;
}

const vec2[4] RECT_VERTICES_COORDS = vec2[] (
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

vec2 rvertexcoord(int id) {
    return RECT_VERTICES_COORDS[id % 4];
}
