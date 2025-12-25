from PIL import Image, ImageDraw
import math

# Create a 192x192 icon with gradient background and waveform bars
size = 192
img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Draw circular gradient background (blue to purple)
center = size // 2
radius = size // 2 - 2

for y in range(size):
    for x in range(size):
        dist = math.sqrt((x - center) ** 2 + (y - center) ** 2)
        if dist <= radius:
            # Gradient from top-left (blue) to bottom-right (purple)
            progress = y / (size - 1)
            c1 = (0, 212, 255)    # Electric blue
            c2 = (123, 47, 255)   # Deep purple
            r = int(c1[0] * (1 - progress) + c2[0] * progress)
            g = int(c1[1] * (1 - progress) + c2[1] * progress)
            b = int(c1[2] * (1 - progress) + c2[2] * progress)
            img.putpixel((x, y), (r, g, b, 255))

# Draw white soundwave bars in center
bar_heights = [0.3, 0.6, 1.0, 0.7, 0.4]  # Relative heights
bar_width = 14
bar_gap = 10
total_width = len(bar_heights) * (bar_width + bar_gap) - bar_gap
start_x = (size - total_width) // 2
max_bar_height = 70

for i, height_ratio in enumerate(bar_heights):
    x = start_x + i * (bar_width + bar_gap)
    bar_height = int(height_ratio * max_bar_height)
    y_top = (size - bar_height) // 2
    y_bottom = y_top + bar_height
    
    # Draw rounded rectangle for each bar
    draw.rounded_rectangle(
        [x, y_top, x + bar_width, y_bottom],
        radius=7,
        fill=(255, 255, 255, 255)
    )

# Save the icon
img.save(r'c:\Users\Muhib\Desktop\Projects\VoiceAI\res\drawable\icon.png', 'PNG')
print("Icon created successfully!")
