package com.dev.delta.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.delta.entities.Category;
import com.dev.delta.entities.ImageModel;
import com.dev.delta.repositories.ImageRepository;
import com.dev.delta.services.CategoryService;

@RestController
@RequestMapping("category")
@CrossOrigin
public class CategoryController {
	@Autowired
	CategoryService categoryService;
	@Autowired

	ImageRepository imageRepository;

	@PostMapping("/create")
	public ResponseEntity<?> addPTToBoard(@Validated @RequestBody Category projectCategory, BindingResult result) {

		if (result.hasErrors()) {
			Map<String, String> errorMap = new HashMap<String, String>();

			for (FieldError error : result.getFieldErrors()) {
				errorMap.put(error.getField(), error.getDefaultMessage());
			}
			return new ResponseEntity<Map<String, String>>(errorMap, HttpStatus.BAD_REQUEST);
		}

		Category newPT = categoryService.saveOrUpdate(projectCategory);

		return new ResponseEntity<Category>(newPT, HttpStatus.CREATED);
	}

	@GetMapping("/all")
	public Iterable<Category> getAllCategorys() {
		return categoryService.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
		Category category = categoryService.findById(id);
		return new ResponseEntity<Category>(category, HttpStatus.OK);
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
		categoryService.delete(id);
		return new ResponseEntity<String>("category was deleted", HttpStatus.OK);
	}

	@PostMapping("/savefile")
	public org.springframework.http.ResponseEntity.BodyBuilder handleFileUpload(
			@RequestParam("file") MultipartFile file) throws IOException {

		System.out.println("Original Image Byte Size - " + file.getBytes().length);

		ImageModel img = new ImageModel(file.getOriginalFilename(), file.getContentType(),

				compressBytes(file.getBytes()));

		imageRepository.save(img);

		return ResponseEntity.status(HttpStatus.OK);
	}

	@GetMapping(path = { "/get/{imageName}" })

	public ImageModel getImage(@PathVariable("imageName") String imageName) throws IOException {

		final Optional<ImageModel> retrievedImage = imageRepository.findByName(imageName);

		ImageModel img = new ImageModel(retrievedImage.get().getName(), retrievedImage.get().getType(),

				decompressBytes(retrievedImage.get().getPicByte()));

		return img;

	}

	public static byte[] compressBytes(byte[] data) {

		Deflater deflater = new Deflater();

		deflater.setInput(data);

		deflater.finish();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

		byte[] buffer = new byte[1024];

		while (!deflater.finished()) {

			int count = deflater.deflate(buffer);

			outputStream.write(buffer, 0, count);
		}
		try {

			outputStream.close();

		} catch (IOException e) {

		}

		System.out.println("Compressed Image Byte Size - " + outputStream.toByteArray().length);

		return outputStream.toByteArray();

	}

	// uncompress the image bytes before returning it to the angular application

	public static byte[] decompressBytes(byte[] data) {

		Inflater inflater = new Inflater();

		inflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

		byte[] buffer = new byte[1024];

		try {

			while (!inflater.finished()) {

				int count = inflater.inflate(buffer);

				outputStream.write(buffer, 0, count);

			}

			outputStream.close();

		} catch (IOException ioe) {

		} catch (DataFormatException e) {

		}

		return outputStream.toByteArray();

	}
}
