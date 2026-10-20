# pull the official base image
FROM node:alpine

# set working direction
WORKDIR /app

# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/node_modules/.bin:$PATH

# install application dependencies
COPY package.json ./
COPY package-lock.json ./
RUN npm i

# create cache directory
RUN mkdir node_modules/.cache && chmod -R 777 node_modules/.cache

# add app
COPY . ./

# start app
# Vite binds to localhost by default, which is unreachable from outside the container.
CMD ["npm", "start", "--", "--host", "0.0.0.0"]
