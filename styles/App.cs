.App {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
}

.card {
  padding: 2em;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin: 20px 0;
  background: white;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

button {
  margin: 5px;
  padding: 10px 15px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
  font-size: 14px;
}

button:hover {
  background: #f5f5f5;
  transform: translateY(-1px);
}

input, textarea {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
}

input:focus, textarea:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0,123,255,0.25);
}

ul {
  text-align: left;
  display: inline-block;
}

li {
  margin: 8px 0;
  list-style-type: none;
}

li:before {
  content: "• ";
  color: #007bff;
  font-weight: bold;
}

/* Responsive tasarım */
@media (max-width: 768px) {
  .App {
    padding: 10px;
  }
  
  .card {
    padding: 1em;
    margin: 10px 0;
  }
}