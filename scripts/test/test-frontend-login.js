// Test script to mimic frontend login behavior
// Node 20+ has built-in fetch

const API_URL = process.env.API_URL || 'http://api-gateway:8082';
const credentials = {
  username: 'ravishah',
  password: 'password'
};

console.log(`Testing login to: ${API_URL}/api/v1/auth/login`);
console.log(`Credentials: ${JSON.stringify(credentials)}`);

fetch(`${API_URL}/api/v1/auth/login`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  body: JSON.stringify(credentials),
})
  .then(async (response) => {
    console.log(`Response status: ${response.status} ${response.statusText}`);
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error(`Error response: ${errorText}`);
      process.exit(1);
    }
    
    return response.json();
  })
  .then((data) => {
    console.log('Login successful!');
    console.log(`Access Token: ${data.accessToken ? 'YES' : 'NO'}`);
    console.log(`Refresh Token: ${data.refreshToken ? 'YES' : 'NO'}`);
    console.log(`Username: ${data.username}`);
    console.log(`Roles: ${JSON.stringify(data.roles)}`);
    console.log(`Session ID: ${data.sessionId}`);
    process.exit(0);
  })
  .catch((error) => {
    console.error('Fetch error:', error.message);
    console.error('Error details:', error);
    process.exit(1);
  });
