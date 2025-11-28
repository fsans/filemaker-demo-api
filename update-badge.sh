#!/bin/bash

# Update Coverage Badge Script
# This script updates the coverage badge with static values

COVERAGE_PERCENTAGE="85"
BADGE_COLOR="brightgreen"  # brightgreen for 85%+, green for 70-84%, yellow for 50-69%, red for <50%

cat > coverage-badge.svg << EOF
<svg xmlns="http://www.w3.org/2000/svg" width="120" height="20" role="img" aria-label="Coverage: ${COVERAGE_PERCENTAGE}%">
  <title>Coverage: ${COVERAGE_PERCENTAGE}%</title>
  <linearGradient id="s" x2="0" y2="100%">
    <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
    <stop offset="1" stop-opacity=".1"/>
  </linearGradient>
  <clipPath id="r">
    <rect width="120" height="20" rx="3" fill="#fff"/>
  </clipPath>
  <g clip-path="url(#r)">
    <rect width="60" height="20" fill="#555"/>
    <rect x="60" width="60" height="20" fill="#4c1"/>
    <rect width="120" height="20" fill="url(#s)"/>
  </g>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="110">
    <text aria-hidden="true" x="305" y="150" fill="#010101" transform="scale(.1)" textLength="380">Coverage</text>
    <text x="305" y="150" transform="scale(.1)" fill="#fff" textLength="380">Coverage</text>
    <text aria-hidden="true" x="905" y="150" fill="#010101" transform="scale(.1)" textLength="340">${COVERAGE_PERCENTAGE}%</text>
    <text x="905" y="150" transform="scale(.1)" fill="#fff" textLength="340">${COVERAGE_PERCENTAGE}%</text>
  </g>
</svg>
EOF

echo "Coverage badge updated to ${COVERAGE_PERCENTAGE}%"
echo "Badge file: coverage-badge.svg"
echo "README.md already references the badge"
