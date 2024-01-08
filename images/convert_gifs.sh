/bin/bash

for i in *mov;
  do ffmpeg -i "$i" -pix_fmt rgb8 -r 10 "${i%.*}.gif"
  && gifsicle -O3 "${i%.*}.gif" -o "${i%.*}.gif";
done