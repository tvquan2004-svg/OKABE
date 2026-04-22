# Serve stage
FROM nginx:alpine

# Copy the pre-built dist folder from the host
# Note: Ensure 'npm run build' is run before building this Docker image
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
